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
import io.maestro3.agent.admin.model.OrganizationModel;
import io.maestro3.agent.admin.model.VmwareShapeConfigDto;
import io.maestro3.agent.dao.IVDCRepository;
import io.maestro3.agent.dao.IVmWareTenantRepository;
import io.maestro3.agent.dao.IVmwareRegionRepository;
import io.maestro3.agent.model.VmwareShape;
import io.maestro3.agent.model.cloud.VCloud;
import io.maestro3.agent.model.vdc.Organization;
import io.maestro3.agent.model.vdc.VDC;
import io.maestro3.diagnostic.service.IHealthCheckService;
import io.maestro3.diagnostic.service.impl.VCloudHealthCheckService;
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

import java.util.Collections;
import java.util.List;

import static io.maestro3.agent.admin.AdminApiConstants.VMWARE_REGION_ENDPOINT;


@RestController
@RequestMapping(VMWARE_REGION_ENDPOINT)
public class OrganizationController {

    private final IAdminCommandFactory adminCommandFactory;
    private IVmwareRegionRepository cloudRepository;
    private IVDCRepository vdcRepository;
    private IVmWareTenantRepository organizationRepository;
    private IHealthCheckService healthCheckService;
    private IM3Signer signer;

    @Autowired
    public OrganizationController(IVmwareRegionRepository cloudRepository, IVDCRepository vdcRepository,
                                  IVmWareTenantRepository organizationRepository, IAdminCommandFactory adminCommandFactory,
                                  VCloudHealthCheckService healthCheckService,
                                  IM3Signer signer) {
        this.cloudRepository = cloudRepository;
        this.healthCheckService = healthCheckService;
        this.vdcRepository = vdcRepository;
        this.adminCommandFactory = adminCommandFactory;
        this.organizationRepository = organizationRepository;
        this.signer = signer;
    }

    @PostMapping("/{regionAlias}/tenant")
    private AdminSdkResponse create(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                    @PathVariable("regionAlias") String regionAlias,
                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<OrganizationModel> command = adminCommandFactory.getCommand(AdminCommandType.VMWARE_CREATE_TENANT);
        OrganizationModel params = command.getParams(decryptedBody, regionAlias);
        return command.execute(params);
    }

    @GetMapping("/{regionAlias}/tenant")
    private AdminSdkResponse getTenantsInRegion(@PathVariable("regionAlias") String regionAlias) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        List<Organization> organizations = organizationRepository.findByRegionIdInCloud(cloud.getId());
        return AdminSdkResponse.of(organizations);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}")
    private AdminSdkResponse getOrganizations(@PathVariable("regionAlias") String regionAlias,
                                              @PathVariable("tenantAlias") String tenantAlias) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        Organization organization = organizationRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, cloud.getId());
        if (organization == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        return AdminSdkResponse.of(organization);
    }

    @DeleteMapping("/{regionAlias}/tenant/{tenantAlias}")
    private AdminSdkResponse deleteOrganization(@PathVariable("regionAlias") String regionAlias,
                                                @PathVariable("tenantAlias") String tenantAlias,
                                                @RequestParam(value = "force", required = false) Boolean force) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        Organization organization = organizationRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, cloud.getId());
        if (organization == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        List<VDC> list = vdcRepository.findByOrganizationIdAndCloudId(organization.getId(), cloud.getId());
        if (CollectionUtils.isNotEmpty(list) && (force == null || !force)) {
            throw new IllegalStateException("ERROR: Tenant contains VDCs and can't be removed");
        }
        organizationRepository.delete(organization);
        return AdminSdkResponse.of("Tenant was successfully removed");
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
        IAdminCommand<VmwareShapeConfigDto> command = adminCommandFactory.getCommand(AdminCommandType.VMWARE_CONFIGURE_SHAPES);
        VmwareShapeConfigDto params = command.getParams(decryptedBody, regionAlias);
        return command.execute(params);
    }

    @DeleteMapping("/{regionAlias}/shape")
    private AdminSdkResponse cleanShapes(@PathVariable("regionAlias") String regionAlias) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        cloud.setAllowedShapes(Collections.emptyList());
        cloudRepository.save(cloud);
        return AdminSdkResponse.of("Shapes were cleaned for region " + regionAlias);
    }

    @GetMapping("/{regionAlias}/shape")
    private AdminSdkResponse getAllowedShapes(@PathVariable("regionAlias") String regionAlias) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        List<VmwareShape> allowedShapes = cloud.getAllowedShapes();
        return AdminSdkResponse.of(allowedShapes == null ? Collections.emptyList() : allowedShapes);
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/management")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("tenantAlias") String tenantAlias,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        OrganizationModel model = JsonUtils.parseJson(decryptedBody, new TypeReference<OrganizationModel>() {});
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        Organization organization = organizationRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, cloud.getId());
        if (organization == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        if (organization.isManagementAvailable() == model.isManagementAvailable()) {
            return AdminSdkResponse.of(organization.isManagementAvailable()
                ? "Management is already enabled"
                : "Management is already disabled");
        }
        organization.setManagementAvailable(model.isManagementAvailable());
        organizationRepository.save(organization);
        return AdminSdkResponse.of(organization.isManagementAvailable()
            ? "Management was enabled"
            : "Management was disabled");
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/management")
    private AdminSdkResponse getManagementState(@PathVariable("regionAlias") String regionAlias,
                                                @PathVariable("tenantAlias") String tenantAlias) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        Organization organization = organizationRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, cloud.getId());
        if (organization == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        return AdminSdkResponse.of(organization.isManagementAvailable()
            ? "Management is enabled"
            : "Management is disabled");
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/vdc")
    private AdminSdkResponse getVdcs(@PathVariable("regionAlias") String regionAlias,
                                     @PathVariable("tenantAlias") String tenantAlias) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        Organization organization = organizationRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, cloud.getId());
        if (organization == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        List<VDC> vdcs = vdcRepository.findByOrganizationIdAndCloudId(
            organization.getId(), organization.getRegionId());
        return AdminSdkResponse.of(vdcs == null ? Collections.emptyList() : vdcs);
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/vdc/{vdcName}")
    private AdminSdkResponse getVdc(@PathVariable("regionAlias") String regionAlias,
                                    @PathVariable("tenantAlias") String tenantAlias,
                                    @PathVariable("vdcName") String vdcName) {
        VCloud cloud = cloudRepository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + regionAlias);
        }
        Organization organization = organizationRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, cloud.getId());
        if (organization == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        VDC vdc = vdcRepository.findByOrganizationIdAndCloudIdAndName(
            organization.getId(), organization.getRegionId(), vdcName);
        return AdminSdkResponse.of(vdc);
    }

    private Organization buildOrganization(OrganizationModel model, VCloud cloud, String organizationHref) {
        Organization organization = new Organization();
        organization.setOrganizationName(model.getOrganizationName());
        organization.setOrganizationHref(organizationHref);
        organization.setRegionId(cloud.getId());
        organization.setTenantAlias(model.getTenant());
        organization.setManagementAvailable(model.isManagementAvailable());
        return organization;
    }
}
