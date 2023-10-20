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
import io.maestro3.agent.admin.model.AdminProjectMetaDto;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class OpenstackCreateAdminProjectMetaCommand extends AbstractAdminCommand<AdminProjectMetaDto> {

    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackCreateAdminProjectMetaCommand(IOpenStackRegionRepository regionService) {
        this.regionService = regionService;
    }

    @Override
    public AdminProjectMetaDto getParams(String body, String... queryParams) {
        AdminProjectMetaDto adminProjectMetaDto = JsonUtils.parseJson(body, AdminProjectMetaDto.class);
        adminProjectMetaDto.setRegionAlias(queryParams[0]);
        return adminProjectMetaDto;
    }

    @Override
    public AdminProjectMetaDto buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        AdminProjectMetaDto adminProjectMetaDto = new AdminProjectMetaDto();
        String regionName = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.REGION_NAME_ITEM);
        String projectDomainId = PrivateWizardUtils.getTextValue(firstStep, OpenstackConfigurationWizardConstant.PROJECT_DOMAIN_ITEM);
        adminProjectMetaDto.setProjectId(projectDomainId);
        adminProjectMetaDto.setRegionAlias(regionName);
        return adminProjectMetaDto;
    }

    @Override
    public AdminSdkResponse execute(AdminProjectMetaDto adminProjectMetaDto) {
        String regionAlias = adminProjectMetaDto.getRegionAlias();
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        EntityValidator.validate(adminProjectMetaDto);
        existingRegion.setAdminProjectMeta(adminProjectMetaDto.toProjectMeta());
        regionService.save(existingRegion);
        return AdminSdkResponse.of("Admin meta successfully configured for region " + regionAlias);
    }

    @Override
    public SdkAdminCommand prepareCommand(AdminProjectMetaDto params) {
        String template = "m3admin private openstack create_admin_project_meta --region_alias ${REGION_ALIAS}" +
            " --project_id ${PROJECT_ID}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("PROJECT_ID", params.getProjectId());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CREATE_ADMIN_PROJECT_META;
    }
}
