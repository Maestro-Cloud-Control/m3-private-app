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

package io.maestro3.agent.admin.factory.impl.nutanix;

import com.fasterxml.jackson.core.type.TypeReference;
import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.IAdminCommand;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.NutanixTenantModel;
import io.maestro3.agent.nutanix.dao.INutanixRegionRepository;
import io.maestro3.agent.nutanix.dao.INutanixTenantRepository;
import io.maestro3.agent.nutanix.model.NutanixRegion;
import io.maestro3.agent.nutanix.model.NutanixTenant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class NutanixSetTenantManagementStatusCommand extends AbstractAdminCommand<NutanixTenantModel> {

    private final INutanixRegionRepository regionRepository;
    private final INutanixTenantRepository tenantRepository;

    @Autowired
    public NutanixSetTenantManagementStatusCommand(INutanixRegionRepository regionRepository,
                                                   INutanixTenantRepository tenantRepository) {
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public NutanixTenantModel getParams(String body, String... queryParams) {
        NutanixTenantModel model = JsonUtils.parseJson(body, new TypeReference<NutanixTenantModel>() {});
        model.setRegionAlias(queryParams[0]);
        model.setTenant(queryParams[1]);
        return model;
    }

    @Override
    public NutanixTenantModel buildRequest(SdkPrivateWizard wizard) {
        return new NutanixTenantModel();// TODO: Andreiev 8/5/2022
    }

    @Override
    public AdminSdkResponse execute(NutanixTenantModel model) {
        String regionAlias = model.getRegionAlias();
        String tenantAlias = model.getTenant();
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
        }
        NutanixTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias " + tenantAlias);
        }
        if (tenant.isManagementAvailable() == model.isManagementAvailable()) {
            return AdminSdkResponse.of(tenant.isManagementAvailable()
                ? "Management is already enabled"
                : "Management is already disabled");
        }
        tenant.setManagementAvailable(model.isManagementAvailable());
        tenantRepository.save(tenant);
        return AdminSdkResponse.of(tenant.isManagementAvailable()
            ? "Management was enabled"
            : "Management was disabled");
    }

    @Override
    public SdkAdminCommand prepareCommand(NutanixTenantModel params) {
        String template = "m3admin private nutanix set_tenant_management_status --region_alias ${REGION_ALIAS}" +
            " --tenant_alias ${TENANT_ALIAS}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("TENANT_ALIAS", params.getTenant());
        if (params.isManagementAvailable()) {
            template += " --available";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.NUTANIX_SET_TENANT_MANAGEMENT_STATUS;
    }
}
