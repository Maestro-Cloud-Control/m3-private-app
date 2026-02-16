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
import io.maestro3.agent.admin.model.NutanixImageModel;
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
import java.util.Objects;


@Component
public class NutanixCreateTenantCommand extends AbstractAdminCommand<NutanixTenantModel> {

    private final INutanixRegionRepository regionRepository;
    private final INutanixTenantRepository tenantRepository;

    @Autowired
    public NutanixCreateTenantCommand(INutanixRegionRepository regionRepository, INutanixTenantRepository tenantRepository) {
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public NutanixTenantModel getParams(String body, String... queryParams) {
        NutanixTenantModel model = JsonUtils.parseJson(body, new TypeReference<NutanixTenantModel>() {});
        model.setRegionAlias(queryParams[0]);
        return model;
    }

    @Override
    protected NutanixTenantModel buildRequest(SdkPrivateWizard wizard) {
        return null;
    }

    @Override
    public AdminSdkResponse execute(NutanixTenantModel model) {
        String regionAlias = model.getRegionAlias();
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (Objects.nonNull(tenantRepository.findByTenantAliasAndRegionIdInCloud(model.getTenant(), region.getId()))) {
            throw new IllegalStateException("ERROR: Nutanix tenant with name is already registered " + model.getTenant());
        }
        NutanixTenant nutanixTenant = new NutanixTenant();
        nutanixTenant.setRegionId(region.getId());
        nutanixTenant.setTenantAlias(model.getTenant());
        tenantRepository.save(nutanixTenant);
        return AdminSdkResponse.of("Tenant registered successfully");
    }

    @Override
    public SdkAdminCommand prepareCommand(NutanixTenantModel params) {
        String template = "m3admin private nutanix create_tenant --region_alias ${REGION_ALIAS} --tenant ${TENANT}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("TENANT", params.getTenant());
        if (params.isManagementAvailable()) {
            template += " --management";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.NUTANIX_CREATE_TENANT;
    }
}
