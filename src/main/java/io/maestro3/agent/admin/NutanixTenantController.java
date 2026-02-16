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

import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.NutanixImageModel;
import io.maestro3.agent.admin.model.NutanixShapeConfigDto;
import io.maestro3.agent.admin.model.NutanixTenantModel;
import io.maestro3.agent.nutanix.dao.INutanixImageRepository;
import io.maestro3.agent.nutanix.dao.INutanixRegionRepository;
import io.maestro3.agent.nutanix.dao.INutanixTenantRepository;
import io.maestro3.agent.nutanix.model.NutanixImage;
import io.maestro3.agent.nutanix.model.NutanixRegion;
import io.maestro3.agent.nutanix.model.NutanixShape;
import io.maestro3.agent.nutanix.model.NutanixTenant;
import io.maestro3.sdk.internal.M3SdkConstants;
import io.maestro3.sdk.internal.signer.IM3Signer;
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

import static io.maestro3.agent.admin.AdminApiConstants.NUTANIX_REGION_ENDPOINT;


@RestController
@RequestMapping(NUTANIX_REGION_ENDPOINT)
public class NutanixTenantController {

    private final IAdminCommandFactory adminCommandFactory;
    private final INutanixImageRepository imageRepository;
    private final INutanixRegionRepository regionRepository;
    private final INutanixTenantRepository tenantRepository;
    private final IM3Signer signer;

    @Autowired
    public NutanixTenantController(IAdminCommandFactory adminCommandFactory,
                                   INutanixRegionRepository regionRepository,
                                   INutanixTenantRepository tenantRepository,
                                   IM3Signer signer,
                                   INutanixImageRepository imageRepository) {
        this.adminCommandFactory = adminCommandFactory;
        this.imageRepository = imageRepository;
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
        this.signer = signer;
    }

    @PostMapping("/{regionAlias}/tenant")
    private AdminSdkResponse create(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                    @PathVariable("regionAlias") String regionAlias,
                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<NutanixTenantModel> command = adminCommandFactory.getCommand(AdminCommandType.NUTANIX_CREATE_REGION);
        NutanixTenantModel model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/tenant")
    private AdminSdkResponse getTenantsInRegion(@PathVariable("regionAlias") String regionAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
        }
        List<NutanixTenant> tenants = tenantRepository.findByRegionIdInCloud(region.getId());
        return AdminSdkResponse.of(tenants);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}")
    private AdminSdkResponse getTenant(@PathVariable("regionAlias") String regionAlias,
                                       @PathVariable("tenantAlias") String tenantAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
        }
        NutanixTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias " + tenantAlias);
        }
        return AdminSdkResponse.of(tenant);
    }

    @DeleteMapping("/{regionAlias}/tenant/{tenantAlias}")
    private AdminSdkResponse deleteTenant(@PathVariable("regionAlias") String regionAlias,
                                          @PathVariable("tenantAlias") String tenantAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
        }
        NutanixTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias " + tenantAlias);
        }
        tenantRepository.delete(tenant);
        return AdminSdkResponse.of("Tenant was successfully removed");
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/image")
    private AdminSdkResponse createImage(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                         @PathVariable("regionAlias") String regionAlias,
                                         @PathVariable("tenantAlias") String tenantAlias,
                                         @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<NutanixImageModel> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CREATE_REGION);
        NutanixImageModel model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/image")
    private AdminSdkResponse listImages(@PathVariable("regionAlias") String regionAlias,
                                        @PathVariable("tenantAlias") String tenantAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (Objects.isNull(region)) {
            throw new IllegalStateException("ERROR: Nutanix region with name is not found received alias is " + regionAlias);
        }
        NutanixTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (Objects.isNull(tenant)) {
            throw new IllegalStateException("ERROR: Nutanix tenant with name is not found received alias is " + tenantAlias);
        }
        List<NutanixImage> images = imageRepository.findImageForTenantInRegion(tenant.getId(), region.getId());
        return AdminSdkResponse.of(images);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/image/{imageAlias}")
    private AdminSdkResponse getImage(@PathVariable("regionAlias") String regionAlias,
                                      @PathVariable("imageAlias") String imageAlias,
                                      @PathVariable("tenantAlias") String tenantAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (Objects.isNull(region)) {
            throw new IllegalStateException("ERROR: Nutanix region with name is not found received alias is " + regionAlias);
        }
        NutanixTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (Objects.isNull(tenant)) {
            throw new IllegalStateException("ERROR: Nutanix tenant with name is not found received alias is " + tenantAlias);
        }
        NutanixImage image = imageRepository.findImageForTenantInRegionByAlias(tenant.getId(), region.getId(), imageAlias);
        return AdminSdkResponse.of(image);
    }

    @DeleteMapping("/{regionAlias}/tenant/{tenantAlias}/image/{imageAlias}")
    private AdminSdkResponse deleteImage(@PathVariable("regionAlias") String regionAlias,
                                         @PathVariable("imageAlias") String imageAlias,
                                         @PathVariable("tenantAlias") String tenantAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (Objects.isNull(region)) {
            throw new IllegalStateException("ERROR: Nutanix region with name is not found received alias is " + regionAlias);
        }
        NutanixTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (Objects.isNull(tenant)) {
            throw new IllegalStateException("ERROR: Nutanix tenant with name is not found received alias is " + tenantAlias);
        }
        boolean isRemoved = imageRepository.deleteImageForTenantInRegionByAlias(tenant.getId(), region.getId(), imageAlias);
        return AdminSdkResponse.of(isRemoved ? "Image was successfully removed" : "Image was not found");
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/check")
    private AdminSdkResponse check(@PathVariable("regionAlias") String regionAlias,
                                   @PathVariable("tenantAlias") String tenantAlias) {
        return AdminSdkResponse.of("Health check is not allowed for nutanix cloud");
    }

    @PostMapping("/{regionAlias}/shape")
    private AdminSdkResponse configureAllowedShapes(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                    @PathVariable("regionAlias") String regionAlias,
                                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<NutanixShapeConfigDto> command = adminCommandFactory.getCommand(AdminCommandType.NUTANIX_SET_REGION_MANAGEMENT);
        NutanixShapeConfigDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @DeleteMapping("/{regionAlias}/shape")
    private AdminSdkResponse cleanShapes(@PathVariable("regionAlias") String regionAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
        }
        region.setAllowedShapes(Collections.emptyList());
        regionRepository.save(region);
        return AdminSdkResponse.of("Shapes were cleaned for region " + regionAlias);
    }

    @GetMapping("/{regionAlias}/shape")
    private AdminSdkResponse getAllowedShapes(@PathVariable("regionAlias") String regionAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
        }
        List<NutanixShape> allowedShapes = region.getAllowedShapes();
        return AdminSdkResponse.of(allowedShapes == null ? Collections.emptyList() : allowedShapes);
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/management")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("tenantAlias") String tenantAlias,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<NutanixTenantModel> command = adminCommandFactory.getCommand(AdminCommandType.NUTANIX_SET_REGION_MANAGEMENT);
        NutanixTenantModel model = command.getParams(decryptedBody, regionAlias, tenantAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/management")
    private AdminSdkResponse getManagementState(@PathVariable("regionAlias") String regionAlias,
                                                @PathVariable("tenantAlias") String tenantAlias) {
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
        }
        NutanixTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias " + tenantAlias);
        }
        return AdminSdkResponse.of(tenant.isManagementAvailable()
            ? "Management is enabled"
            : "Management is disabled");
    }
}
