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
import io.maestro3.agent.admin.model.NutanixRegionModel;
import io.maestro3.agent.nutanix.dao.INutanixRegionRepository;
import io.maestro3.agent.nutanix.model.NutanixRegion;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class NutanixSetRegionManagementCommand extends AbstractAdminCommand<NutanixRegionModel> {

    private final INutanixRegionRepository repository;

    @Autowired
    public NutanixSetRegionManagementCommand(INutanixRegionRepository repository) {
        this.repository = repository;
    }

    @Override
    public NutanixRegionModel getParams(String body, String... queryParams) {
        NutanixRegionModel model = JsonUtils.parseJson(body, new TypeReference<NutanixRegionModel>() {});
        model.setRegionAlias(queryParams[0]);
        return model;
    }

    @Override
    protected NutanixRegionModel buildRequest(SdkPrivateWizard wizard) {
        return null;
    }

    @Override
    public AdminSdkResponse execute(NutanixRegionModel model) {
        String regionAlias = model.getRegionAlias();
        NutanixRegion region = repository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Region not found by region alias " + regionAlias);
        }
        if (region.isManagementAvailable() == model.isManagementAvailable()) {
            return AdminSdkResponse.of(region.isManagementAvailable()
                ? "Management is already enabled"
                : "Management is already disabled");
        }
        region.setManagementAvailable(model.isManagementAvailable());
        repository.save(region);
        return AdminSdkResponse.of(region.isManagementAvailable()
            ? "Management was enabled"
            : "Management was disabled");
    }

    @Override
    public SdkAdminCommand prepareCommand(NutanixRegionModel params) {
        String template = "m3admin private nutanix set_region_management_status --region_alias ${REGION_ALIAS}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        if (params.isManagementAvailable()) {
            template += " --available";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.NUTANIX_SET_REGION_MANAGEMENT;
    }
}
