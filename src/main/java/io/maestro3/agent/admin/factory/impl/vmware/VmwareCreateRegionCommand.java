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

package io.maestro3.agent.admin.factory.impl.vmware;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.VCloudModel;
import io.maestro3.agent.client.IVCloudHttpClient;
import io.maestro3.agent.client.VCloudDirectorApiConstants;
import io.maestro3.agent.dao.IVmwareRegionRepository;
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.model.cloud.VCloud;
import io.maestro3.agent.service.configuration.VmwareConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.internal.util.StringUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkSelectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class VmwareCreateRegionCommand extends AbstractAdminCommand<VCloudModel> {

    private final IVmwareRegionRepository repository;
    private final IVCloudHttpClient cloudHttpClient;

    @Autowired
    public VmwareCreateRegionCommand(IVmwareRegionRepository repository, IVCloudHttpClient cloudHttpClient) {
        this.repository = repository;
        this.cloudHttpClient = cloudHttpClient;
    }

    @Override
    public VCloudModel getParams(String body, String... queryParams) {
        return JsonUtils.parseJson(body, VCloudModel.class);
    }

    @Override
    public VCloudModel buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        String server = PrivateWizardUtils.getTextValue(firstStep, VmwareConfigurationWizardConstant.SERVER_ITEM);
        String user = PrivateWizardUtils.getTextValue(firstStep, VmwareConfigurationWizardConstant.USERNAME);
        String password = PrivateWizardUtils.getTextValue(firstStep, VmwareConfigurationWizardConstant.PASSWORD_ITEM);
        String regionName = PrivateWizardUtils.getTextValue(secondStep, VmwareConfigurationWizardConstant.REGION_NAME_ITEM);
        String apiVersion = PrivateWizardUtils.getSelectedOptionItem(firstStep.getData().getSelect(),
            VmwareConfigurationWizardConstant.API_VERSION_ITEM).getValue();
        SdkSelectItem managementItem = PrivateWizardUtils.getSelectItem(secondStep, VmwareConfigurationWizardConstant.ENABLE_MANAGEMENT_ITEM);
        SdkSelectItem scriptItem = PrivateWizardUtils.getSelectItem(secondStep, VmwareConfigurationWizardConstant.INIT_SCRIPT_ITEM);
        VCloudModel model = new VCloudModel();
        model.setApiVersion(apiVersion);
        model.setServer(server);
        model.setPassword(password);
        model.setUsername(user);
        model.setRegionAlias(regionName);
        model.setManagementAvailable(managementItem.getOption().get(0).getSelected());
        model.setInitScript(scriptItem.getOption().get(0).getSelected());
        return model;
    }

    @Override
    public AdminSdkResponse execute(VCloudModel model) {
        VCloud cloud = new VCloud(model.getServer(),
            String.format(VCloudDirectorApiConstants.ACCEPT_HEADER_VALUE_FORMAT, model.getApiVersion()),
            model.getRegionAlias(),
            model.getUsername(),
            model.getPassword(),
            model.isManagementAvailable(),
            null,
            model.isInitScript());
        IRegion byRegionAlias = repository.findByAliasInCloud(cloud.getRegionAlias());
        if (byRegionAlias != null) {
            throw new IllegalStateException("ERROR: Region with specified alias already exist");
        }
        String authToken = cloudHttpClient.getAuthToken(cloud, false);
        if (StringUtils.isBlank(authToken)) {
            throw new IllegalStateException("ERROR: User credentials are not from user from system organization");
        }
        repository.save(cloud);
        return AdminSdkResponse.of("Region registered successfully");
    }

    @Override
    public SdkAdminCommand prepareCommand(VCloudModel params) {
        String template = "m3admin private vmware create_region " +
            "--server ${VMWARE_ADDRESS} " +
            "--api_version ${API_VERSION} " +
            "--region_alias ${REGION_NAME} " +
            "--username ${AUTH_USER} " +
            "--password ${PASSWORD} ";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("VMWARE_ADDRESS", params.getServer());
        placeholders.put("API_VERSION", params.getApiVersion());
        placeholders.put("REGION_NAME", params.getRegionAlias());
        placeholders.put("AUTH_USER", params.getUsername());
        placeholders.put("PASSWORD", params.getPassword());
        if (params.isManagementAvailable()) {
            template += "--management ";
        }
        if (params.isInitScript()) {
            template += "--init_script ";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.VMWARE_CREATE_REGION;
    }

}
