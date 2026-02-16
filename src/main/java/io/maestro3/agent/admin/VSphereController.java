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

import com.fasterxml.jackson.core.type.TypeReference;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.VCloudModel;
import io.maestro3.agent.admin.model.VSphereModel;
import io.maestro3.agent.vsphere.dao.IVSphereRegionRepository;
import io.maestro3.agent.vsphere.dao.IVSphereTenantRepository;
import io.maestro3.agent.vsphere.model.VSphere;
import io.maestro3.agent.vsphere.model.VSphereTenant;
import io.maestro3.sdk.internal.M3SdkConstants;
import io.maestro3.sdk.internal.signer.IM3Signer;
import io.maestro3.sdk.internal.util.CollectionUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
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

import static io.maestro3.agent.admin.AdminApiConstants.VSPHERE_REGION_ENDPOINT;


@RestController
@RequestMapping(VSPHERE_REGION_ENDPOINT)
public class VSphereController {

    private final IAdminCommandFactory adminCommandFactory;
    private IVSphereRegionRepository repository;
    private IVSphereTenantRepository tenantRepository;
    private IM3Signer signer;

    @Autowired
    public VSphereController(IAdminCommandFactory adminCommandFactory,
                             IVSphereRegionRepository repository,
                             IVSphereTenantRepository tenantRepository, IM3Signer signer) {
        this.repository = repository;
        this.adminCommandFactory = adminCommandFactory;
        this.tenantRepository = tenantRepository;
        this.signer = signer;
    }

    @PostMapping
    private AdminSdkResponse create(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<VSphereModel> command = adminCommandFactory.getCommand(AdminCommandType.VSPHERE_CREATE_REGION);
        VSphereModel params = command.getParams(decryptedBody);
        return command.execute(params);
    }

    @GetMapping
    private AdminSdkResponse getRegions() {
        List<VSphere> allRegions = repository.findAllRegionsForCloud();
        allRegions.forEach(r -> {
            r.setUsername(AdminApiConstants.TEMP_CREDENTIALS);
            r.setPassword(AdminApiConstants.TEMP_CREDENTIALS);
        });
        return AdminSdkResponse.of(allRegions);
    }

    @GetMapping("/{regionAlias}")
    private AdminSdkResponse getRegionByd(@PathVariable("regionAlias") String regionAlias) {
        VSphere vSphere = repository.findByAliasInCloud(regionAlias);
        if (vSphere == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        vSphere.setUsername(AdminApiConstants.TEMP_CREDENTIALS);
        vSphere.setPassword(AdminApiConstants.TEMP_CREDENTIALS);
        return AdminSdkResponse.of(vSphere);
    }

    @DeleteMapping("/{regionAlias}")
    private AdminSdkResponse removeRegionByd(@PathVariable("regionAlias") String regionAlias,
                                             @RequestParam(value = "force", required = false) Boolean force) {
        VSphere vSphere = repository.findByAliasInCloud(regionAlias);
        if (vSphere == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        List<VSphereTenant> list = tenantRepository.findByRegionIdInCloud(vSphere.getId());
        if (CollectionUtils.isNotEmpty(list) && (force == null || !force)) {
            throw new IllegalStateException("ERROR: VCloud contains tenants and can't be removed ");
        }
        repository.delete(vSphere);
        return AdminSdkResponse.of("Region removed successfully");
    }

    @PostMapping("/{regionAlias}/management")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        VCloudModel model = JsonUtils.parseJson(decryptedBody, new TypeReference<VCloudModel>() {
        });
        VSphere vSphere = repository.findByAliasInCloud(regionAlias);
        if (vSphere == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        if (vSphere.isManagementAvailable() == model.isManagementAvailable()) {
            return AdminSdkResponse.of(vSphere.isManagementAvailable()
                ? "Management is already enabled"
                : "Management is already disabled");
        }
        vSphere.setManagementAvailable(model.isManagementAvailable());
        repository.save(vSphere);
        return AdminSdkResponse.of(vSphere.isManagementAvailable()
            ? "Management was enabled"
            : "Management was disabled");
    }

    @GetMapping("/{regionAlias}/management")
    private AdminSdkResponse getManagementState(@PathVariable("regionAlias") String regionAlias) {
        VSphere vSphere = repository.findByAliasInCloud(regionAlias);
        if (vSphere == null) {
            throw new IllegalArgumentException("Vcloud not found by region alias " + regionAlias);
        }
        return AdminSdkResponse.of(vSphere.isManagementAvailable()
            ? "Management is enabled"
            : "Management is disabled");
    }

}
