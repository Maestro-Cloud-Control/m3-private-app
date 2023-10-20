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
import io.maestro3.agent.admin.model.TenantDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.dao.IOpenStackTenantRepository;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.model.tenant.OpenStackTenant;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkOptionItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkSelectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class OpenstackSetTenantManagementCommand extends AbstractAdminCommand<TenantDto> {

    private final IOpenStackRegionRepository regionService;
    private final IOpenStackTenantRepository tenantRepository;

    @Autowired
    public OpenstackSetTenantManagementCommand(IOpenStackRegionRepository regionService,
                                               IOpenStackTenantRepository tenantRepository) {
        this.regionService = regionService;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public TenantDto getParams(String body, String... queryParams) {
        TenantDto userInfoDto = JsonUtils.parseJson(body, TenantDto.class);
        userInfoDto.setRegionAlias(queryParams[0]);
        userInfoDto.setNameAlias(queryParams[1]);
        return userInfoDto;
    }

    @Override
    public TenantDto buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkOptionItem regionOption = PrivateWizardUtils.getSelectedOptionItem(firstStep.getData().getSelect(), OpenstackConfigurationWizardConstant.REGION_SELECT_ITEM);
        String regionName = regionOption.getValue();
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, OpenstackConfigurationWizardConstant.TENANT_NAME);
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        SdkSelectItem managementEnableItem = PrivateWizardUtils.getSelectItem(thirdStep, OpenstackConfigurationWizardConstant.ENABLE_MANAGEMENT_ITEM);
        Boolean managementEnable = managementEnableItem.getOption().get(0).getSelected();
        TenantDto conf = new TenantDto();
        conf.setRegionAlias(regionName);
        conf.setNameAlias(tenantName);
        conf.setManagementAvailable(Boolean.TRUE.equals(managementEnable));
        return conf;
    }

    @Override
    public AdminSdkResponse execute(TenantDto tenantDto) {
        String regionAlias = tenantDto.getRegionAlias();
        String tenantAlias = tenantDto.getNameAlias();
        OpenStackRegionConfig region = regionService.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Region is not exist with alias  " + regionAlias);
        }
        OpenStackTenant existingTenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (existingTenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        if (existingTenant.isManagementAvailable() == tenantDto.isManagementAvailable()) {
            return AdminSdkResponse.of(existingTenant.isManagementAvailable()
                ? "Management is already enabled"
                : "Management is already disabled");
        }
        existingTenant.setManagementAvailable(tenantDto.isManagementAvailable());
        tenantRepository.save(existingTenant);
        return AdminSdkResponse.of(existingTenant.isManagementAvailable()
            ? "Management was enabled"
            : "Management was disabled");
    }

    @Override
    public SdkAdminCommand prepareCommand(TenantDto params) {
        String template = "m3admin private openstack set_tenant_management_status"
            + " --region_alias ${REGION_ALIAS}"
            + " --tenant_alias ${TENANT_ALIAS}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("TENANT_ALIAS", params.getNameAlias());
        if (params.isManagementAvailable()) {
            template += " --available";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_SET_TENANT_MANAGEMENT;
    }
}
