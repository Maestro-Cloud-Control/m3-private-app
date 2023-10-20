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
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.TenantDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.base.TenantState;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.model.tenant.OpenStackTenant;
import io.maestro3.agent.service.DbServicesProvider;
import io.maestro3.agent.service.TenantDbService;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkOptionItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class OpenstackCreateTenantCommand extends AbstractAdminCommand<TenantDto> {

    private final DbServicesProvider dbServicesProvider;
    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackCreateTenantCommand(DbServicesProvider dbServicesProvider,
                                        IOpenStackRegionRepository regionService) {
        this.dbServicesProvider = dbServicesProvider;
        this.regionService = regionService;
    }

    @Override
    public TenantDto getParams(String body, String... queryParams) {
        TenantDto tenantDto = JsonUtils.parseJson(body, TenantDto.class);
        tenantDto.setRegionAlias(queryParams[0]);
        return tenantDto;
    }

    @Override
    public TenantDto buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        SdkOptionItem regionOption = PrivateWizardUtils.getSelectedOptionItem(firstStep.getData().getSelect(), OpenstackConfigurationWizardConstant.REGION_SELECT_ITEM);
        String regionName = regionOption.getValue();
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, OpenstackConfigurationWizardConstant.TENANT_NAME);
        String projectId = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.PROJECT_ID_ITEM);
        String projectDomain = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.PROJECT_DOMAIN_ITEM);
        String projectName = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.PROJECT_ITEM);

        SdkOptionItem networkItem = PrivateWizardUtils.getSelectedOptionItem(thirdStep.getData().getSelect(), OpenstackConfigurationWizardConstant.NETWORK_SELECT_ITEM);
        String networkId = networkItem.getValue();

        SdkOptionItem groupItem = PrivateWizardUtils.getSelectedOptionItem(thirdStep.getData().getSelect(), OpenstackConfigurationWizardConstant.SECURITY_GROUP_SELECT_ITEM);
        String groupName = groupItem.getTitle();
        String groupId = groupItem.getValue();

        return new TenantDto(projectId, networkId, projectName, groupId, groupName, tenantName, projectDomain, regionName);
    }

    @Override
    public AdminSdkResponse execute(TenantDto tenantDto) {
        String regionAlias = tenantDto.getRegionAlias();
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        EntityValidator.validate(tenantDto);
        TenantDbService tenantDbService = dbServicesProvider.getTenantDbService();
        String regionId = existingRegion.getId();
        OpenStackTenant existingTenant = tenantDbService
            .findOpenStackTenantByNameAndRegion(tenantDto.getNameAlias(), regionId);
        if (existingTenant != null) {
            throw new IllegalStateException("ERROR: Tenant with specified alias already exist. Received name " + tenantDto.getNameAlias());
        }
        OpenStackTenant config = tenantDto.toTenantConfig(regionId);
        config.setTenantState(TenantState.AVAILABLE);
        tenantDbService.save(config);
        return AdminSdkResponse.of("Tenant was successfully added to region " + regionAlias);
    }

    @Override
    public SdkAdminCommand prepareCommand(TenantDto params) {
        String template = "m3admin private openstack create_tenant --region_alias ${REGION_ALIAS} --native_id ${NATIVE_ID}" +
            " --network_id ${NETWORK_ID} --domain ${DOMAIN_NAME}" +
            " --native_name ${NATIVE_NAME} --name_alias ${NAME_ALIAS}" +
            " --security_group ${SECURITY_GROUP_NAME} --security_group_id ${SECURITY_GROUP_ID}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("NATIVE_ID", params.getNativeId());
        placeholders.put("DOMAIN_NAME", params.getDomainName());
        placeholders.put("NETWORK_ID", params.getNetworkId());
        placeholders.put("NATIVE_NAME", params.getNativeName());
        placeholders.put("NAME_ALIAS", params.getNameAlias());
        placeholders.put("SECURITY_GROUP_NAME", params.getSecurityGroupName());
        placeholders.put("SECURITY_GROUP_ID", params.getSecurityGroupId());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CREATE_TENANT;
    }
}
