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
import io.maestro3.agent.admin.model.VSphereModel;
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.agent.vsphere.client.IVSphereHttpClient;
import io.maestro3.agent.vsphere.dao.IVSphereRegionRepository;
import io.maestro3.agent.vsphere.model.RegionType;
import io.maestro3.agent.vsphere.model.VSphere;
import io.maestro3.agent.vsphere.service.configuration.VSphereConfigurationWizardConstant;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.internal.util.StringUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkOptionItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkSelectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class VSphereCreateRegionCommand extends AbstractAdminCommand<VSphereModel> {

    private final IVSphereRegionRepository repository;
    private final IVSphereHttpClient cloudHttpClient;

    @Autowired
    public VSphereCreateRegionCommand(IVSphereRegionRepository repository, IVSphereHttpClient cloudHttpClient) {
        this.repository = repository;
        this.cloudHttpClient = cloudHttpClient;
    }

    @Override
    public VSphereModel getParams(String body, String... queryParams) {
        return JsonUtils.parseJson(body, VSphereModel.class);
    }

    @Override
    public VSphereModel buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        String server = PrivateWizardUtils.getTextValue(firstStep, VSphereConfigurationWizardConstant.SERVER_ITEM);
        String user = PrivateWizardUtils.getTextValue(firstStep, VSphereConfigurationWizardConstant.USERNAME);
        String password = PrivateWizardUtils.getTextValue(firstStep, VSphereConfigurationWizardConstant.PASSWORD_ITEM);
        String regionName = PrivateWizardUtils.getTextValue(secondStep, VSphereConfigurationWizardConstant.REGION_NAME_ITEM);
        String regionTypeName = PrivateWizardUtils.getSelectedOptionItem(firstStep.getData().getSelect(),
            VSphereConfigurationWizardConstant.REGION_TYPE_SELECT_ITEM).getValue();
        RegionType type = RegionType.valueOf(regionTypeName);
        SdkSelectItem managementItem = PrivateWizardUtils.getSelectItem(secondStep, VSphereConfigurationWizardConstant.ENABLE_MANAGEMENT_ITEM);
        VSphereModel model = new VSphereModel();
        model.setServer(server);
        model.setRegionType(type);
        model.setPassword(password);
        model.setUsername(user);
        model.setRegionAlias(regionName);
        model.setManagementAvailable(managementItem.getOption().get(0).getSelected());
        SdkOptionItem datacenterItem = null;
        if (type != RegionType.VCENTER){
            SdkSelectItem selectItem = PrivateWizardUtils.getSelectItem(thirdStep, VSphereConfigurationWizardConstant.DATACENTER_SELECT_ITEM);
            datacenterItem = PrivateWizardUtils.getSelectedOptionItem(selectItem.getOption());
            String datacenterId = datacenterItem.getValue();
            model.setDatacenter(datacenterId);
        }
        if (type == RegionType.CLUSTER){
            if (datacenterItem == null) {
                throw new IllegalArgumentException();
            }
            SdkOptionItem clusterItem = PrivateWizardUtils.getSelectedOptionItem(datacenterItem.getSelect(), VSphereConfigurationWizardConstant.CLUSTER_SELECT_ITEM);
            String clusterID = clusterItem.getValue();
            model.setCluster(clusterID);
        }
        return model;
    }

    @Override
    public AdminSdkResponse execute(VSphereModel model) {
        VSphere cloud = new VSphere(model.getServer(),
            model.getRegionAlias(),
            model.getUsername(),
            model.getPassword(),
            model.isManagementAvailable());
        cloud.setRegionType(model.getRegionType());
        cloud.setCluster(model.getCluster());
        cloud.setDatacenter(model.getDatacenter());
        IRegion byRegionAlias = repository.findByAliasInCloud(cloud.getRegionAlias());
        if (byRegionAlias != null) {
            throw new IllegalStateException("ERROR: Region with specified alias already exist");
        }
        String authToken = cloudHttpClient.getAuthToken(cloud, false);
        if (StringUtils.isBlank(authToken)) {
            throw new IllegalStateException("ERROR: User credentials are not valid");
        }
        repository.save(cloud);
        return AdminSdkResponse.of("Region registered successfully");
    }

    @Override
    public SdkAdminCommand prepareCommand(VSphereModel params) {
        String template = "m3admin private vsphere create_region " +
            "--server ${VMWARE_ADDRESS} " +
            "--region_alias ${REGION_NAME} " +
            "--username ${AUTH_USER} " +
            "--password ${PASSWORD} " +
            "--region_type ${REGION_TYPE} ";
        if (StringUtils.isNotBlank(params.getDatacenter())) {
            template += "--datacenter_id ${DATACENTER} ";
        }
        if (StringUtils.isNotBlank(params.getCluster())) {
            template += "--cluster_id ${CLUSTER} ";
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("VMWARE_ADDRESS", params.getServer());
        placeholders.put("REGION_NAME", params.getRegionAlias());
        placeholders.put("AUTH_USER", params.getUsername());
        placeholders.put("PASSWORD", params.getPassword());
        placeholders.put("REGION_TYPE", params.getRegionType().name());
        placeholders.put("DATACENTER", params.getDatacenter());
        placeholders.put("CLUSTER", params.getCluster());
        if (params.isManagementAvailable()) {
            template += "--management";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.VSPHERE_CREATE_REGION;
    }

}
