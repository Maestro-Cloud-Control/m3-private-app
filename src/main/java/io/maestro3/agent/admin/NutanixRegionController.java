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

package io.maestro3.agent.admin;

import io.maestro3.agent.admin.factory.impl.AdminCommandFactory;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.NutanixRegionModel;
import io.maestro3.agent.nutanix.dao.INutanixRegionRepository;
import io.maestro3.agent.nutanix.dao.INutanixTenantRepository;
import io.maestro3.agent.nutanix.model.NutanixRegion;
import io.maestro3.agent.nutanix.model.NutanixTenant;
import io.maestro3.agent.nutanix.service.INutanixWebhookService;
import io.maestro3.sdk.internal.M3SdkConstants;
import io.maestro3.sdk.internal.signer.IM3Signer;
import io.maestro3.sdk.internal.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static io.maestro3.agent.admin.AdminApiConstants.NUTANIX_REGION_ENDPOINT;


@RestController
@RequestMapping(NUTANIX_REGION_ENDPOINT)
public class NutanixRegionController {

    private final AdminCommandFactory adminCommandFactory;
    private final INutanixRegionRepository repository;
    private final INutanixTenantRepository tenantRepository;
    private final IM3Signer signer;
    private final INutanixWebhookService webhookService;

    @Autowired
    public NutanixRegionController(AdminCommandFactory adminCommandFactory,
                                   INutanixRegionRepository repository,
                                   INutanixTenantRepository tenantRepository,
                                   IM3Signer signer,
                                   INutanixWebhookService webhookService) {
        this.adminCommandFactory = adminCommandFactory;
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.signer = signer;
        this.webhookService = webhookService;
    }

    @PostMapping
    private AdminSdkResponse create(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<NutanixRegionModel> command = adminCommandFactory.getCommand(AdminCommandType.NUTANIX_CREATE_REGION);
        NutanixRegionModel model = command.getParams(decryptedBody);
        return command.execute(model);
    }

    @GetMapping
    private AdminSdkResponse getRegions() {
        List<NutanixRegion> allRegions = repository.findAllRegionsForCloud();
        allRegions.forEach(r -> {
            r.setUsername(AdminApiConstants.TEMP_CREDENTIALS);
            r.setPassword(AdminApiConstants.TEMP_CREDENTIALS);
            r.setElementPassword(AdminApiConstants.TEMP_CREDENTIALS);
            r.setElementUsername(AdminApiConstants.TEMP_CREDENTIALS);
        });
        return AdminSdkResponse.of(allRegions);
    }

    @GetMapping("/{regionAlias}")
    private AdminSdkResponse getRegionByd(@PathVariable("regionAlias") String regionAlias) {
        NutanixRegion region = repository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Region not found by region alias " + regionAlias);
        }
        region.setUsername(AdminApiConstants.TEMP_CREDENTIALS);
        region.setPassword(AdminApiConstants.TEMP_CREDENTIALS);
        region.setElementPassword(AdminApiConstants.TEMP_CREDENTIALS);
        region.setElementUsername(AdminApiConstants.TEMP_CREDENTIALS);
        return AdminSdkResponse.of(region);
    }

    @DeleteMapping("/{regionAlias}")
    private AdminSdkResponse removeRegionByd(@PathVariable("regionAlias") String regionAlias,
                                             @RequestParam(value = "force", required = false) Boolean force) {
        NutanixRegion region = repository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Region not found by region alias " + regionAlias);
        }
        List<NutanixTenant> list = tenantRepository.findByRegionIdInCloud(region.getId());
        if (CollectionUtils.isNotEmpty(list) && (force == null || !force)) {
            throw new IllegalStateException("ERROR: Region contains tenants and can't be removed ");
        }
        webhookService.deleteHook(region);
        repository.delete(region);
        return AdminSdkResponse.of("Region removed successfully");
    }

    @PostMapping("/{regionAlias}/management")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<NutanixRegionModel> command = adminCommandFactory.getCommand(AdminCommandType.NUTANIX_SET_REGION_MANAGEMENT);
        NutanixRegionModel model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/management")
    private AdminSdkResponse getManagementState(@PathVariable("regionAlias") String regionAlias) {
        NutanixRegion region = repository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalArgumentException("Vcloud not found by region alias " + regionAlias);
        }
        return AdminSdkResponse.of(region.isManagementAvailable()
            ? "Management is enabled"
            : "Management is disabled");
    }

}
