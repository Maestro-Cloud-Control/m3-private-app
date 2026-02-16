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
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.RabbitNotificationConfigDto;
import io.maestro3.agent.admin.model.TemplateModel;
import io.maestro3.agent.admin.model.VCloudModel;
import io.maestro3.agent.amqp.listener.IAmqpHelperService;
import io.maestro3.agent.dao.IVAppTemplateRepository;
import io.maestro3.agent.dao.IVmWareTenantRepository;
import io.maestro3.agent.dao.IVmwareRegionRepository;
import io.maestro3.agent.model.base.RabbitNotificationConfig;
import io.maestro3.agent.model.cloud.VCloud;
import io.maestro3.agent.model.template.VAppTemplate;
import io.maestro3.agent.model.vdc.Organization;
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
import java.util.Optional;

import static io.maestro3.agent.admin.AdminApiConstants.VMWARE_REGION_ENDPOINT;


@RestController
@RequestMapping(VMWARE_REGION_ENDPOINT)
public class VCloudController {

    private final IAdminCommandFactory adminCommandFactory;
    private IVmwareRegionRepository repository;
    private IVmWareTenantRepository tenantRepository;
    private IVAppTemplateRepository vAppTemplateRepository;
    private IM3Signer signer;
    private IAmqpHelperService amqpHelperService;

    @Autowired
    public VCloudController(IVmwareRegionRepository repository,
                            IVmWareTenantRepository tenantRepository, IAmqpHelperService amqpHelperService,
                            IVAppTemplateRepository vAppTemplateRepository, IAdminCommandFactory adminCommandFactory,
                            IM3Signer signer) {
        this.tenantRepository = tenantRepository;
        this.repository = repository;
        this.adminCommandFactory = adminCommandFactory;
        this.amqpHelperService = amqpHelperService;
        this.vAppTemplateRepository = vAppTemplateRepository;
        this.signer = signer;
    }

    @PostMapping
    private AdminSdkResponse create(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                    @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<VCloudModel> command = adminCommandFactory.getCommand(AdminCommandType.VMWARE_CREATE_REGION);
        VCloudModel params = command.getParams(decryptedBody);
        return command.execute(params);
    }

    @GetMapping
    private AdminSdkResponse getRegions() {
        List<VCloud> allRegions = repository.findAllRegionsForCloud();
        allRegions.forEach(r -> {
            r.setUsername(AdminApiConstants.TEMP_CREDENTIALS);
            r.setPassword(AdminApiConstants.TEMP_CREDENTIALS);
            RabbitNotificationConfig config = r.getRabbitNotificationConfig();
            if (config != null) {
                config.setRabbitUsername(AdminApiConstants.TEMP_CREDENTIALS);
                config.setRabbitPassword(AdminApiConstants.TEMP_CREDENTIALS);
            }
        });
        return AdminSdkResponse.of(allRegions);
    }

    @GetMapping("/{regionAlias}")
    private AdminSdkResponse getRegionByd(@PathVariable("regionAlias") String regionAlias) {
        VCloud vCloud = repository.findByAliasInCloud(regionAlias);
        if (vCloud == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        vCloud.setUsername(AdminApiConstants.TEMP_CREDENTIALS);
        vCloud.setPassword(AdminApiConstants.TEMP_CREDENTIALS);
        RabbitNotificationConfig config = vCloud.getRabbitNotificationConfig();
        if (config != null) {
            config.setRabbitUsername(AdminApiConstants.TEMP_CREDENTIALS);
            config.setRabbitPassword(AdminApiConstants.TEMP_CREDENTIALS);
        }
        return AdminSdkResponse.of(vCloud);
    }

    @DeleteMapping("/{regionAlias}")
    private AdminSdkResponse removeRegionByd(@PathVariable("regionAlias") String regionAlias,
                                             @RequestParam(value = "force", required = false) Boolean force) {
        VCloud vCloud = repository.findByAliasInCloud(regionAlias);
        if (vCloud == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        List<Organization> list = tenantRepository.findByRegionIdInCloud(vCloud.getId());
        if (CollectionUtils.isNotEmpty(list) && (force == null || !force)) {
            throw new IllegalStateException("ERROR: VCloud contains tenants and can't be removed ");
        }
        repository.delete(vCloud);
        return AdminSdkResponse.of("Region removed successfully");
    }

    @PostMapping("/{regionAlias}/management")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        VCloudModel model = JsonUtils.parseJson(decryptedBody, new TypeReference<VCloudModel>() {
        });
        VCloud vCloud = repository.findByAliasInCloud(regionAlias);
        if (vCloud == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        if (vCloud.isManagementAvailable() == model.isManagementAvailable()) {
            return AdminSdkResponse.of(vCloud.isManagementAvailable()
                ? "Management is already enabled"
                : "Management is already disabled");
        }
        vCloud.setManagementAvailable(model.isManagementAvailable());
        repository.save(vCloud);
        return AdminSdkResponse.of(vCloud.isManagementAvailable()
            ? "Management was enabled"
            : "Management was disabled");
    }

    @PostMapping("/{regionAlias}/rabbit")
    private AdminSdkResponse setRabbitConfig(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                             @PathVariable("regionAlias") String regionAlias,
                                             @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        RabbitNotificationConfigDto configDto = JsonUtils.parseJson(decryptedBody, RabbitNotificationConfigDto.class);
        EntityValidator.validateConfig(configDto);
        VCloud vCloud = repository.findByAliasInCloud(regionAlias);
        if (vCloud == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        RabbitNotificationConfig config = configDto.toConfig();
        amqpHelperService.assertExchangeExist(config);
        vCloud.setRabbitNotificationConfig(config);
        repository.save(vCloud);
        return AdminSdkResponse.of("Configuration was successfully set." +
            " Please note, that configuration will be applied after agent restart.");
    }

    @GetMapping("/{regionAlias}/rabbit")
    private AdminSdkResponse getRabbitConfig(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                             @PathVariable("regionAlias") String regionAlias) {
        VCloud vCloud = repository.findByAliasInCloud(regionAlias);
        if (vCloud == null) {
            throw new IllegalStateException("ERROR: VCloud not found by region alias " + regionAlias);
        }
        RabbitNotificationConfig config = vCloud.getRabbitNotificationConfig();
        if (config != null) {
            config.setRabbitUsername(AdminApiConstants.TEMP_CREDENTIALS);
            config.setRabbitPassword(AdminApiConstants.TEMP_CREDENTIALS);
        }
        return AdminSdkResponse.of(config == null ? null : new RabbitNotificationConfigDto(config));
    }

    @GetMapping("/{regionAlias}/management")
    private AdminSdkResponse getManagementState(@PathVariable("regionAlias") String regionAlias) {
        VCloud vCloud = repository.findByAliasInCloud(regionAlias);
        if (vCloud == null) {
            throw new IllegalArgumentException("Vcloud not found by region alias " + regionAlias);
        }
        return AdminSdkResponse.of(vCloud.isManagementAvailable()
            ? "Management is enabled"
            : "Management is disabled");
    }

    @GetMapping("/{regionAlias}/template")
    private AdminSdkResponse getTemplates(@PathVariable("regionAlias") String regionAlias) {
        VCloud cloud = repository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalArgumentException("Vcloud not found by region alias " + regionAlias);
        }
        List<VAppTemplate> templateList = vAppTemplateRepository.findByCloudId(cloud.getId());
        return AdminSdkResponse.of(templateList == null ? Collections.emptyList() : templateList);
    }

    @PostMapping("/{regionAlias}/template")
    private AdminSdkResponse createTemplates(@PathVariable("regionAlias") String regionAlias,
                                             @RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                             @RequestBody String body) {
        String decrypt = signer.decrypt(body, authKey);
        IAdminCommand<TemplateModel> command = adminCommandFactory.getCommand(AdminCommandType.VMWARE_UPLOAD_TEMPLATE);
        TemplateModel params = command.getParams(decrypt, regionAlias);
        return command.execute(params);
    }

    @GetMapping("/{regionAlias}/template/{templateId}")
    private AdminSdkResponse getTemplates(@PathVariable("regionAlias") String regionAlias,
                                          @PathVariable("templateId") String templateId) {
        VCloud cloud = repository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalArgumentException("vCloud not found by region alias " + regionAlias);
        }
        Optional<VAppTemplate> templateOptional = vAppTemplateRepository.findById(templateId);
        if (!templateOptional.isPresent()) {
            throw new IllegalArgumentException("vApp template not found by id " + templateId);
        }
        return AdminSdkResponse.of(templateOptional.get());
    }

    @DeleteMapping("/{regionAlias}/template/{templateId}")
    private AdminSdkResponse deleteTemplate(@PathVariable("regionAlias") String regionAlias,
                                            @PathVariable("templateId") String templateId,
                                            @RequestParam(value = "force", required = false) Boolean force) {
        VCloud cloud = repository.findByAliasInCloud(regionAlias);
        if (cloud == null) {
            throw new IllegalArgumentException("vCloud not found by region alias " + regionAlias);
        }
        Optional<VAppTemplate> templateOptional = vAppTemplateRepository.findById(templateId);
        if (!templateOptional.isPresent()) {
            throw new IllegalArgumentException("vApp template not found by id " + templateId);
        }
        vAppTemplateRepository.delete(templateOptional.get());
        return AdminSdkResponse.of("Template was removed");
    }

    @PostMapping("/{regionAlias}/template/{templateId}")
    private AdminSdkResponse setTemplateAlias(@PathVariable("regionAlias") String regionAlias,
                                              @PathVariable("templateId") String templateId,
                                              @RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                              @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        TemplateModel model = JsonUtils.parseJson(decryptedBody, new TypeReference<TemplateModel>() {
        });
        VCloud vCloud = repository.findByAliasInCloud(regionAlias);
        if (vCloud == null) {
            throw new IllegalArgumentException("vCloud not found by region alias " + regionAlias);
        }
        Optional<VAppTemplate> templateOptional = vAppTemplateRepository.findById(templateId);
        if (!templateOptional.isPresent()) {
            throw new IllegalArgumentException("vApp template not found by id " + templateId);
        }
        VAppTemplate vAppTemplate = templateOptional.get();
        vAppTemplate.setAlias(model.getAlias());
        vAppTemplateRepository.save(vAppTemplate);
        return AdminSdkResponse.of("Alias was successfully set to template");
    }
}
