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
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.nutanix.dao.INutanixRegionRepository;
import io.maestro3.agent.nutanix.model.NutanixRegion;
import io.maestro3.agent.nutanix.service.INutanixWebhookService;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class NutanixCreateRegionCommand extends AbstractAdminCommand<NutanixRegionModel> {

    private final INutanixRegionRepository repository;
    private final INutanixWebhookService webhookService;

    @Autowired
    public NutanixCreateRegionCommand(INutanixRegionRepository repository,
                                      INutanixWebhookService webhookService) {
        this.repository = repository;
        this.webhookService = webhookService;
    }

    @Override
    public NutanixRegionModel getParams(String body, String... params) {
        return JsonUtils.parseJson(body, new TypeReference<NutanixRegionModel>() {});
    }

    @Override
    protected NutanixRegionModel buildRequest(SdkPrivateWizard wizard) {
        return null;
    }

    @Override
    public AdminSdkResponse execute(NutanixRegionModel model) {
        NutanixRegion region = new NutanixRegion();
        region.setServer(model.getServer());
        region.setUsername(model.getUsername());
        region.setPassword(model.getPassword());
        region.setElementServer(model.getElementServer());
        region.setElementUsername(model.getElementUsername());
        region.setElementPassword(model.getElementPassword());
        region.setRegionAlias(model.getRegionAlias());
        region.setClusterName(model.getClusterName());
        region.setManagementAvailable(model.isManagementAvailable());
        region.setClusterUuid(model.getClusterUuid());
        IRegion byRegionAlias = repository.findByAliasInCloud(region.getRegionAlias());
        if (byRegionAlias != null) {
            throw new IllegalStateException("ERROR: Region with specified alias already exist");
        }
        repository.save(region);
        webhookService.createHook(region);
        return AdminSdkResponse.of("Region registered successfully");
    }

    @Override
    public SdkAdminCommand prepareCommand(NutanixRegionModel params) {
        String template = "m3admin private nutanix create_region --server ${SERVER} --region_alias ${REGION_ALIAS}" +
            " --username ${USERNAME} --password ${PASSWORD} --element_server ${ELEMENT_SERVER}" +
            " --element_username ${ELEMENT_USERNAME} --element_password ${ELEMENT_PASSWORD}" +
            " --cluster ${CLUSTER} --cluster_uuid ${CLUSTER_UUID}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("SERVER", params.getServer());
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("USERNAME", params.getUsername());
        placeholders.put("PASSWORD", params.getPassword());
        placeholders.put("ELEMENT_SERVER", params.getElementServer());
        placeholders.put("ELEMENT_USERNAME", params.getElementUsername());
        placeholders.put("ELEMENT_PASSWORD", params.getElementPassword());
        placeholders.put("CLUSTER", params.getClusterName());
        placeholders.put("CLUSTER_UUID", params.getClusterUuid());
        if (params.isManagementAvailable()) {
            template += " --management";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.NUTANIX_CREATE_REGION;
    }
}
