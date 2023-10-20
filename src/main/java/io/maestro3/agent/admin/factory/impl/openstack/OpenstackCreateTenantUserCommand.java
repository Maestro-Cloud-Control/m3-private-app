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
import io.maestro3.agent.admin.model.UserInfoDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
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
public class OpenstackCreateTenantUserCommand extends AbstractAdminCommand<UserInfoDto> {

    private final DbServicesProvider dbServicesProvider;
    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackCreateTenantUserCommand(DbServicesProvider dbServicesProvider, IOpenStackRegionRepository regionService) {
        this.dbServicesProvider = dbServicesProvider;
        this.regionService = regionService;
    }

    @Override
    public UserInfoDto getParams(String body, String... queryParams) {
        UserInfoDto userInfoDto = JsonUtils.parseJson(body, UserInfoDto.class);
        userInfoDto.setRegionAlias(queryParams[0]);
        userInfoDto.setTenantAlias(queryParams[1]);
        return userInfoDto;
    }

    @Override
    public UserInfoDto buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkOptionItem regionOption = PrivateWizardUtils.getSelectedOptionItem(firstStep.getData().getSelect(), OpenstackConfigurationWizardConstant.REGION_SELECT_ITEM);
        String regionName = regionOption.getValue();
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, OpenstackConfigurationWizardConstant.TENANT_NAME);
        String username = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.USERNAME_ITEM);
        String password = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.PASSWORD_ITEM);
        String userDomain = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.USER_DOMAIN_ITEM);
        String userId = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.USER_ID);
        return new UserInfoDto(userId, username, password, userDomain, tenantName, regionName);
    }

    @Override
    public AdminSdkResponse execute(UserInfoDto userInfoDto) {
        String regionAlias = userInfoDto.getRegionAlias();
        String tenantAlias = userInfoDto.getTenantAlias();
        EntityValidator.validate(userInfoDto);
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        TenantDbService tenantDbService = dbServicesProvider.getTenantDbService();
        OpenStackTenant existingTenant = tenantDbService.findOpenStackTenantByNameAndRegion(tenantAlias, existingRegion.getId());
        if (existingTenant == null) {
            throw new IllegalStateException("ERROR: Tenant with specified alias is not exist. Received name " + tenantAlias);
        }
        existingTenant.setUserInfo(userInfoDto.toUserInfo());
        tenantDbService.save(existingTenant);
        return AdminSdkResponse.of("User was successfully added to region " + regionAlias);
    }

    @Override
    public SdkAdminCommand prepareCommand(UserInfoDto params) {
        String template = "m3admin private openstack create_tenant_user "
            + "--region_alias ${REGION_ALIAS} "
            + "--name ${TENANT_USERNAME} "
            + "--password ${TENANT_USER_PASS} "
            + "--native_id ${TENANT_USER_ID} "
            + "--tenant_alias ${TENANT_ALIAS} "
            + "--domain ${TENANT_USER_DOMAIN_NAME}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("TENANT_ALIAS", params.getTenantAlias());
        placeholders.put("TENANT_USERNAME", params.getName());
        placeholders.put("TENANT_USER_PASS", params.getPassword());
        placeholders.put("TENANT_USER_ID", params.getNativeId());
        placeholders.put("TENANT_USER_DOMAIN_NAME", params.getDomainName());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CREATE_TENANT_USER;
    }
}
