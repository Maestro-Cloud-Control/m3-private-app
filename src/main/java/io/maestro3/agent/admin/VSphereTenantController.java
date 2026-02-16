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
import io.maestro3.agent.admin.model.VSphereAddNetworkModel;
import io.maestro3.agent.admin.model.VSphereImageModel;
import io.maestro3.agent.admin.model.VSphereShapeConfigDto;
import io.maestro3.agent.admin.model.VSphereTenantModel;
import io.maestro3.agent.vsphere.dao.IVSphereRegionRepository;
import io.maestro3.agent.vsphere.dao.IVSphereTenantRepository;
import io.maestro3.agent.vsphere.model.PlacementType;
import io.maestro3.agent.vsphere.model.VSphere;
import io.maestro3.agent.vsphere.model.VSphereShape;
import io.maestro3.agent.vsphere.model.VSphereTenant;
import io.maestro3.diagnostic.service.IHealthCheckService;
import io.maestro3.diagnostic.service.impl.VSphereHealthCheckService;
import io.maestro3.sdk.internal.M3SdkConstants;
import io.maestro3.sdk.internal.signer.IM3Signer;
import io.maestro3.sdk.internal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static io.maestro3.agent.admin.AdminApiConstants.VSPHERE_REGION_ENDPOINT;


@RestController
@RequestMapping(VSPHERE_REGION_ENDPOINT)
public class VSphereTenantController {

    private final IAdminCommandFactory adminCommandFactory;
    private IVSphereRegionRepository sphereRegionRepository;
    private IVSphereTenantRepository tenantRepository;
    private IHealthCheckService healthCheckService;
    private IM3Signer signer;

    @Autowired
    public VSphereTenantController(IVSphereRegionRepository sphereRegionRepository, IVSphereTenantRepository tenantRepository,
                                   VSphereHealthCheckService healthCheckService, IM3Signer signer, IAdminCommandFactory adminCommandFactory) {
        this.sphereRegionRepository = sphereRegionRepository;
        this.tenantRepository = tenantRepository;
        this.adminCommandFactory = adminCommandFactory;
        this.healthCheckService = healthCheckService;
        this.signer = signer;
    }

    @PostMapping("/{regionAlias}/tenant")
    private AdminSdkResponse create(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                    @PathVariable("regionAlias") String regionAlias,
                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<VSphereTenantModel> command = adminCommandFactory.getCommand(AdminCommandType.VSPHERE_CREATE_TENANT);
        VSphereTenantModel params = command.getParams(decryptedBody, regionAlias);
        return command.execute(params);
    }

    @GetMapping("/{regionAlias}/tenant")
    private AdminSdkResponse getTenantsInRegion(@PathVariable("regionAlias") String regionAlias) {
        VSphere sphere = sphereRegionRepository.findByAliasInCloud(regionAlias);
        if (sphere == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        List<VSphereTenant> tenants = tenantRepository.findByRegionIdInCloud(sphere.getId());
        return AdminSdkResponse.of(tenants);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}")
    private AdminSdkResponse getTenant(@PathVariable("regionAlias") String regionAlias,
                                       @PathVariable("tenantAlias") String tenantAlias) {
        VSphere sphere = sphereRegionRepository.findByAliasInCloud(regionAlias);
        if (sphere == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        VSphereTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, sphere.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        return AdminSdkResponse.of(tenant);
    }

    @DeleteMapping("/{regionAlias}/tenant/{tenantAlias}")
    private AdminSdkResponse deleteTenant(@PathVariable("regionAlias") String regionAlias,
                                          @PathVariable("tenantAlias") String tenantAlias) {
        VSphere sphere = sphereRegionRepository.findByAliasInCloud(regionAlias);
        if (sphere == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        VSphereTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, sphere.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        tenantRepository.delete(tenant);
        return AdminSdkResponse.of("Tenant was successfully removed");
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/image")
    private AdminSdkResponse createImage(@PathVariable("tenantAlias") String tenantAlias,
                                         @PathVariable("regionAlias") String regionAlias,
                                         @RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                         @RequestBody String body) {
        String decrypt = signer.decrypt(body, authKey);
        IAdminCommand<VSphereImageModel> command = adminCommandFactory.getCommand(AdminCommandType.VSPHERE_CREATE_IMAGE);
        VSphereImageModel params = command.getParams(decrypt, regionAlias, tenantAlias);
        return command.execute(params);
    }

    @PostMapping("/{regionAlias}/network")
    private AdminSdkResponse createNetwork(@PathVariable("regionAlias") String regionAlias,
                                           @RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                           @RequestBody String body) {
        String decrypt = signer.decrypt(body, authKey);
        IAdminCommand<VSphereAddNetworkModel> command = adminCommandFactory.getCommand(AdminCommandType.VSPHERE_ADD_NETWORK);
        VSphereAddNetworkModel params = command.getParams(decrypt, regionAlias);
        return command.execute(params);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/check")
    private AdminSdkResponse check(@PathVariable("regionAlias") String regionAlias,
                                   @PathVariable("tenantAlias") String tenantAlias) {
        healthCheckService.checkTenantInRegionByAliases(tenantAlias, regionAlias);
        return AdminSdkResponse.of("Organization configuration is ok");
    }

    @PostMapping("/{regionAlias}/shape")
    private AdminSdkResponse configureAllowedShapes(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                    @PathVariable("regionAlias") String regionAlias,
                                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<VSphereShapeConfigDto> command = adminCommandFactory.getCommand(AdminCommandType.VSPHERE_CONFIGURE_SHAPES);
        VSphereShapeConfigDto params = command.getParams(decryptedBody, regionAlias);
        return command.execute(params);
    }

    @DeleteMapping("/{regionAlias}/shape")
    private AdminSdkResponse cleanShapes(@PathVariable("regionAlias") String regionAlias) {
        VSphere sphere = sphereRegionRepository.findByAliasInCloud(regionAlias);
        if (sphere == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        sphere.setAllowedShapes(Collections.emptyList());
        sphereRegionRepository.save(sphere);
        return AdminSdkResponse.of("Shapes were cleaned for region " + regionAlias);
    }

    @GetMapping("/{regionAlias}/shape")
    private AdminSdkResponse getAllowedShapes(@PathVariable("regionAlias") String regionAlias) {
        VSphere sphere = sphereRegionRepository.findByAliasInCloud(regionAlias);
        if (sphere == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        List<VSphereShape> allowedShapes = sphere.getAllowedShapes();
        return AdminSdkResponse.of(allowedShapes == null ? Collections.emptyList() : allowedShapes);
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/management")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("tenantAlias") String tenantAlias,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        VSphereTenantModel model = JsonUtils.parseJson(decryptedBody, new TypeReference<VSphereTenantModel>() {
        });
        VSphere sphere = sphereRegionRepository.findByAliasInCloud(regionAlias);
        if (sphere == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        VSphereTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, sphere.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
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

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/management")
    private AdminSdkResponse getManagementState(@PathVariable("regionAlias") String regionAlias,
                                                @PathVariable("tenantAlias") String tenantAlias) {
        VSphere sphere = sphereRegionRepository.findByAliasInCloud(regionAlias);
        if (sphere == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        VSphereTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, sphere.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        return AdminSdkResponse.of(tenant.isManagementAvailable()
            ? "Management is enabled"
            : "Management is disabled");
    }
}
