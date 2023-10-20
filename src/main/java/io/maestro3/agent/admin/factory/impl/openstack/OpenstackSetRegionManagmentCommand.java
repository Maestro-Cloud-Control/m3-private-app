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

package io.maestro3.agent.admin.factory.impl.openstack;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkSelectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class OpenstackSetRegionManagmentCommand extends AbstractAdminCommand<OpenStackRegionConfig> {

    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackSetRegionManagmentCommand(IOpenStackRegionRepository regionService) {
        this.regionService = regionService;
    }

    @Override
    public OpenStackRegionConfig getParams(String body, String... queryParams) {
        OpenStackRegionConfig userInfoDto = JsonUtils.parseJson(body, OpenStackRegionConfig.class);
        userInfoDto.setRegionAlias(queryParams[0]);
        return userInfoDto;
    }

    @Override
    public OpenStackRegionConfig buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkSelectItem managementEnableItem = PrivateWizardUtils.getSelectItem(secondStep, OpenstackConfigurationWizardConstant.ENABLE_MANAGEMENT_ITEM);
        Boolean managementEnable = managementEnableItem.getOption().get(0).getSelected();
        String regionName = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.REGION_NAME_ITEM);
        OpenStackRegionConfig conf = new OpenStackRegionConfig();
        conf.setRegionAlias(regionName);
        conf.setManagementAvailable(Boolean.TRUE.equals(managementEnable));
        return conf;
    }

    @Override
    public AdminSdkResponse execute(OpenStackRegionConfig userInfoDto) {
        String regionAlias = userInfoDto.getRegionAlias();
        OpenStackRegionConfig region = regionService.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        if (region.isManagementAvailable() == userInfoDto.isManagementAvailable()) {
            return AdminSdkResponse.of(region.isManagementAvailable()
                ? "Management is already enabled"
                : "Management is already disabled");
        }
        region.setManagementAvailable(userInfoDto.isManagementAvailable());
        regionService.save(region);
        return AdminSdkResponse.of(region.isManagementAvailable()
            ? "Management was enabled"
            : "Management was disabled");
    }

    @Override
    public SdkAdminCommand prepareCommand(OpenStackRegionConfig params) {
        String template = "m3admin private openstack set_region_management_status --region_alias ${REGION_ALIAS}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        if (params.isManagementAvailable()) {
            template += " --available";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_SET_REGION_MANAGEMENT;
    }
}
