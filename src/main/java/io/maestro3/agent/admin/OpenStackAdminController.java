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

import io.maestro3.agent.admin.model.AdminProjectMetaDto;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.ImageDto;
import io.maestro3.agent.admin.model.PlatformShapeMappingDto;
import io.maestro3.agent.admin.model.RabbitConfigDto;
import io.maestro3.agent.admin.model.RegionConfigDto;
import io.maestro3.agent.admin.model.ServiceTenantDto;
import io.maestro3.agent.admin.model.ShapeConfigDto;
import io.maestro3.agent.admin.model.TenantDto;
import io.maestro3.agent.admin.model.UserInfoDto;
import io.maestro3.agent.admin.model.security.request.ConfigureSecurityModeRequest;
import io.maestro3.agent.admin.model.security.request.DeleteSecurityModeRequest;
import io.maestro3.agent.admin.model.security.request.DescribeSecurityModesRequest;
import io.maestro3.agent.admin.model.security.request.SetSecurityModeRequest;
import io.maestro3.agent.amqp.router.IAmqpRoutingService;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.AdminProjectMeta;
import io.maestro3.agent.model.OpenStackUserInfo;
import io.maestro3.agent.model.base.RabbitNotificationConfig;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.model.region.OpenStackServiceTenantInfo;
import io.maestro3.agent.model.tenant.OpenStackTenant;
import io.maestro3.agent.service.DbServicesProvider;
import io.maestro3.agent.service.TenantDbService;
import io.maestro3.sdk.internal.M3SdkConstants;
import io.maestro3.sdk.internal.signer.IM3Signer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.request.agent.ConfigureTenantNetworkRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping(AdminApiConstants.OS_REGION_ENDPOINT)
public class OpenStackAdminController {

    private final IAdminCommandFactory adminCommandFactory;
    private final IM3Signer signer;
    private final DbServicesProvider dbServicesProvider;
    private final IOpenStackRegionRepository regionService;
    private final IAmqpRoutingService routerService;

    @Autowired
    public OpenStackAdminController(IAdminCommandFactory adminCommandFactory,
                                    IM3Signer signer,
                                    DbServicesProvider dbServicesProvider,
                                    IAmqpRoutingService routerService,
                                    IOpenStackRegionRepository regionService) {
        this.adminCommandFactory = adminCommandFactory;
        this.signer = signer;
        this.regionService = regionService;
        this.routerService = routerService;
        this.dbServicesProvider = dbServicesProvider;
    }

    @GetMapping
    public AdminSdkResponse getRegionsInfo() {
        Collection<OpenStackRegionConfig> regions = regionService.findAllRegionsForCloud();
        List<RegionConfigDto> regionConfigDtos = regions.stream()
                .map(RegionConfigDto::new)
                .collect(Collectors.toList());
        return AdminSdkResponse.of(regionConfigDtos);
    }

    @PostMapping
    public AdminSdkResponse createRegion(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                         @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<RegionConfigDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CREATE_REGION);
        RegionConfigDto model = command.getParams(decryptedBody);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}")
    public AdminSdkResponse getRegionInfo(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig region = regionService.findByAliasInCloud(regionAlias);
        return AdminSdkResponse.of(region == null ? null : new RegionConfigDto(region));
    }

    @GetMapping("/{regionAlias}/serviceTenant")
    public AdminSdkResponse getServiceTenantInfo(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        OpenStackServiceTenantInfo serviceTenantInfo = existingRegion.getServiceTenantInfo();
        return AdminSdkResponse.of(serviceTenantInfo == null ? null : new ServiceTenantDto(serviceTenantInfo));
    }

    @PostMapping("/{regionAlias}/serviceTenant")
    public AdminSdkResponse getServiceTenantInfo(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                 @PathVariable("regionAlias") String regionAlias,
                                                 @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<ServiceTenantDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_SET_SERVICE_TENANT);
        ServiceTenantDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/user")
    public AdminSdkResponse getUserFromRegion(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        OpenStackUserInfo adminUserCredentials = existingRegion.getAdminUserCredentials();
        return AdminSdkResponse.of(adminUserCredentials == null ? null : new UserInfoDto(adminUserCredentials));
    }

    @PostMapping("/{regionAlias}/user")
    public AdminSdkResponse setUserToRegion(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                            @PathVariable("regionAlias") String regionAlias,
                                            @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<UserInfoDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_SET_USER);
        UserInfoDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/adminProjectMeta")
    public AdminSdkResponse getAdminMetaFromRegion(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        AdminProjectMeta adminProjectMeta = existingRegion.getAdminProjectMeta();
        return AdminSdkResponse.of(adminProjectMeta == null ? null : new AdminProjectMetaDto(adminProjectMeta));
    }

    @PostMapping("/{regionAlias}/adminProjectMeta")
    public AdminSdkResponse setAdminMetaToRegion(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                 @PathVariable("regionAlias") String regionAlias,
                                                 @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<AdminProjectMetaDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CREATE_ADMIN_PROJECT_META);
        AdminProjectMetaDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @PostMapping("/{regionAlias}/adminProjectMeta/image")
    public AdminSdkResponse setImageToAdminProject(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                   @PathVariable("regionAlias") String regionAlias,
                                                   @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<PlatformShapeMappingDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_ADD_PROJECT_META_IMAGE);
        PlatformShapeMappingDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/rabbit")
    public AdminSdkResponse getRabbitConfig(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        RabbitNotificationConfig notificationConfig = existingRegion.getRabbitNotificationConfig();
        return AdminSdkResponse.of(notificationConfig == null ? null : new RabbitConfigDto(notificationConfig));
    }

    @PostMapping("/{regionAlias}/rabbit")
    public AdminSdkResponse setRabbitConfig(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                            @PathVariable("regionAlias") String regionAlias,
                                            @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        RabbitConfigDto configDto = JsonUtils.parseJson(decryptedBody, RabbitConfigDto.class);
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        EntityValidator.validate(configDto);
        RabbitNotificationConfig rabbitNotificationConfig = configDto.toNotificationConfig(
                routerService.getOpenStackNovaNotificationsQueue(),
                routerService.getOpenStackCinderNotificationsQueue(),
                routerService.getOpenStackGlanceNotificationsQueue()
        );
        existingRegion.setRabbitNotificationConfig(rabbitNotificationConfig);
        regionService.save(existingRegion);
        return AdminSdkResponse.of("Rabbit was successfully configured for region " + regionAlias);
    }

    @GetMapping("/{regionAlias}/image")
    public AdminSdkResponse getImagesFromRegion(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        List<ImageDto> images = dbServicesProvider.getMachineImageDbService().findByRegionId(existingRegion.getId())
                .stream()
                .map(i -> new ImageDto(i, regionAlias))
                .collect(Collectors.toList());
        return AdminSdkResponse.of(images);
    }

    @PostMapping("/{regionAlias}/image")
    public AdminSdkResponse setImageToRegion(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                             @PathVariable("regionAlias") String regionAlias,
                                             @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<ImageDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CREATE_IMAGE);
        ImageDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @PostMapping("/{regionAlias}/management")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<OpenStackRegionConfig> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_SET_REGION_MANAGEMENT);
        OpenStackRegionConfig model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/shape")
    public AdminSdkResponse getShapesFromRegion(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        List<ShapeConfigDto> flavorConfigs = existingRegion.getAllowedShapes()
                .stream()
                .map(ShapeConfigDto::new)
                .collect(Collectors.toList());
        return AdminSdkResponse.of(flavorConfigs);
    }

    @PostMapping("/{regionAlias}/shape")
    public AdminSdkResponse setShapesToRegion(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                              @PathVariable("regionAlias") String regionAlias,
                                              @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<ShapeConfigDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CREATE_SHAPE);
        ShapeConfigDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }

    @GetMapping("/{regionAlias}/tenant")
    public AdminSdkResponse getTenants(@PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        Collection<TenantDto> tenantConfigs = dbServicesProvider.getTenantDbService().findAllByRegion(existingRegion.getId())
                .stream()
                .map(TenantDto::new)
                .collect(Collectors.toList());
        return AdminSdkResponse.of(tenantConfigs);
    }

    @PostMapping("/{regionAlias}/tenant")
    public AdminSdkResponse createTenant(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                         @PathVariable("regionAlias") String regionAlias,
                                         @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<TenantDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CREATE_TENANT);
        TenantDto model = command.getParams(decryptedBody, regionAlias);
        return command.execute(model);
    }


    @GetMapping("/{regionAlias}/tenant/{tenantAlias}")
    public AdminSdkResponse getTenant(@PathVariable("regionAlias") String regionAlias,
                                      @PathVariable("tenantAlias") String tenantAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        TenantDbService tenantDbService = dbServicesProvider.getTenantDbService();
        String regionId = existingRegion.getId();
        OpenStackTenant existingTenant = tenantDbService
                .findOpenStackTenantByNameAndRegion(tenantAlias, regionId);
        return AdminSdkResponse.of(existingTenant == null ? null : new TenantDto(existingTenant));
    }

    @GetMapping("/{regionAlias}/tenant/{tenantAlias}/user")
    public AdminSdkResponse getUserFromTenant(@PathVariable("tenantAlias") String tenantAlias,
                                              @PathVariable("regionAlias") String regionAlias) {
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        TenantDbService tenantDbService = dbServicesProvider.getTenantDbService();
        OpenStackTenant existingTenant = tenantDbService
                .findOpenStackTenantByNameAndRegion(tenantAlias, existingRegion.getId());
        if (existingTenant == null) {
            throw new IllegalStateException("ERROR: Tenant with specified alias is not exist. Received name " + tenantAlias);
        }
        OpenStackUserInfo userInfo = existingTenant.getUserInfo();
        return AdminSdkResponse.of(userInfo == null ? null : new UserInfoDto(userInfo));
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/user")
    public AdminSdkResponse setUserToTenant(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                            @PathVariable("regionAlias") String regionAlias,
                                            @PathVariable("tenantAlias") String tenantAlias,
                                            @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<UserInfoDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CREATE_TENANT_USER);
        UserInfoDto model = command.getParams(decryptedBody, regionAlias, tenantAlias);
        return command.execute(model);
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/management")
    public AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                      @PathVariable("tenantAlias") String tenantAlias,
                                                      @PathVariable("regionAlias") String regionAlias,
                                                      @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<TenantDto> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_SET_TENANT_MANAGEMENT);
        TenantDto model = command.getParams(decryptedBody, regionAlias, tenantAlias);
        return command.execute(model);
    }

    @PostMapping("/{regionAlias}/tenant/{tenantAlias}/network/configure")
    public AdminSdkResponse configureTenantNetwork(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                   @PathVariable("tenantAlias") String tenantAlias,
                                                   @PathVariable("regionAlias") String regionAlias,
                                                   @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<ConfigureTenantNetworkRequest> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CONFIGURE_TENANT_NETWORK);
        ConfigureTenantNetworkRequest model = command.getParams(decryptedBody, regionAlias, tenantAlias);
        return command.execute(model);
    }

    @PostMapping("/{regionAlias}/network/security")
    public AdminSdkResponse configureSecurityMode(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                  @PathVariable("regionAlias") String regionAlias,
                                                  @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<ConfigureSecurityModeRequest> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_CONFIGURE_SECURITY_MODE);
        ConfigureSecurityModeRequest request = command.getParams(decryptedBody, regionAlias);
        return command.execute(request);
    }

    @DeleteMapping("/{regionAlias}/network/security")
    public AdminSdkResponse deleteSecurityMode(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                               @PathVariable("regionAlias") String regionAlias,
                                               @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<DeleteSecurityModeRequest> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_DELETE_SECURITY_MODE);
        DeleteSecurityModeRequest request = command.getParams(decryptedBody, regionAlias);
        return command.execute(request);
    }

    @GetMapping("/network/security")
    public AdminSdkResponse describeSecurityModes(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                  @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<DescribeSecurityModesRequest> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_DESCRIBE_SECURITY_MODES);
        DescribeSecurityModesRequest request = command.getParams(decryptedBody);
        return command.execute(request);
    }

    @PostMapping("/{regionAlias}/tenant/network/security")
    public AdminSdkResponse setSecurityMode(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                            @PathVariable("regionAlias") String regionAlias,
                                            @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<SetSecurityModeRequest> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_SET_SECURITY_MODE);
        SetSecurityModeRequest request = command.getParams(decryptedBody, regionAlias);
        return command.execute(request);
    }
}
