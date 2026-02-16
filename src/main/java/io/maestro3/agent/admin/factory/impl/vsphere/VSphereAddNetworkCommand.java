/*
 * Copyright 2023 Maestro Cloud Control LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.maestro3.agent.admin.factory.impl.vsphere;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.VSphereAddNetworkModel;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.agent.vsphere.dao.IVSphereImageRepository;
import io.maestro3.agent.vsphere.dao.IVSphereRegionRepository;
import io.maestro3.agent.vsphere.dao.IVSphereTenantRepository;
import io.maestro3.agent.vsphere.model.DeployType;
import io.maestro3.agent.vsphere.model.NetworkPortgroup;
import io.maestro3.agent.vsphere.model.VSphere;
import io.maestro3.agent.vsphere.model.VSphereNetworkModel;
import io.maestro3.agent.vsphere.model.VSphereTenant;
import io.maestro3.agent.vsphere.model.image.VsphereImage;
import io.maestro3.agent.vsphere.service.IVSphereInfrastructureService;
import io.maestro3.agent.vsphere.service.configuration.VSphereConfigurationWizardConstant;
import io.maestro3.sdk.internal.util.Assert;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableRowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class VSphereAddNetworkCommand extends AbstractAdminCommand<VSphereAddNetworkModel> {

    private final IVSphereRegionRepository repository;
    private final IVSphereInfrastructureService infrastructureService;

    @Autowired
    public VSphereAddNetworkCommand(IVSphereRegionRepository repository,
                                    IVSphereInfrastructureService infrastructureService) {
        this.repository = repository;
        this.infrastructureService = infrastructureService;
    }

    @Override
    public VSphereAddNetworkModel getParams(String body, String... queryParams) {
        VSphereAddNetworkModel shapeConfigDto = JsonUtils.parseJson(body, VSphereAddNetworkModel.class);
        shapeConfigDto.setRegion(queryParams[0]);
        return shapeConfigDto;
    }

    @Override
    protected VSphereAddNetworkModel buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException("Single command generation is not supported");
    }

    @Override
    public List<VSphereAddNetworkModel> buildRequests(SdkPrivateWizard wizard) {
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkPrivateStep step = PrivateWizardUtils.getStepById(4, wizard.getStep());
        String regionName = PrivateWizardUtils.getTextValue(secondStep, VSphereConfigurationWizardConstant.REGION_NAME_ITEM);
        SdkTableItem networkTable = PrivateWizardUtils.findItem(step.getData().getTable(),
            VSphereConfigurationWizardConstant.NETWORK_TABLE, SdkTableItem::getName);
        List<SdkTableRowItem> networkRows = PrivateWizardUtils.getSelectedRowForTable(networkTable).stream()
            .filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
            .collect(Collectors.toList());
        List<VSphereAddNetworkModel> models = new ArrayList<>();
        for (SdkTableRowItem networkRow : networkRows) {
            String id = networkRow.getHiddenData().get("id");
            String type = networkRow.getHiddenData().get("type");
            VSphereAddNetworkModel model = new VSphereAddNetworkModel();
            model.setRegion(regionName);
            model.setId(id);
            model.setType(type);
            models.add(model);
        }
        return models;
    }

    @Override
    public AdminSdkResponse execute(VSphereAddNetworkModel model) {
        VSphere region = repository.findByAliasInCloud(model.getRegion());
        if (region == null) {
            throw new IllegalArgumentException("Region not found by region alias " + model.getRegion());
        }
        List<VSphereNetworkModel> networkModels = infrastructureService.listNetworks(region);
        boolean isExist = networkModels.stream()
            .anyMatch(existingNet -> existingNet.getId().equals(model.getId()));
        if (!isExist){
            throw new IllegalArgumentException("Network can't be found by id " + model.getId());
        }
        region.getNetwork().put(model.getId(), NetworkPortgroup.valueOf(model.getType()));
        repository.save(region);
        return AdminSdkResponse.of("Network was successfully added");
    }

    @Override
    public SdkAdminCommand prepareCommand(VSphereAddNetworkModel params) {
        Map<String, String> placeholders = new HashMap<>();
        String template = "m3admin private vsphere add_network " +
            "--region_alias ${REGION_NAME} " +
            "--network_id ${NET_ID} " +
            "--network_type ${NET_TYPE} ";
        placeholders.put("REGION_NAME", params.getRegion());
        placeholders.put("NET_ID", String.valueOf(params.getId()));
        placeholders.put("NET_TYPE", String.valueOf(params.getType()));
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.VSPHERE_ADD_NETWORK;
    }

}
