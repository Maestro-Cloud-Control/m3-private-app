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

package io.maestro3.agent.admin.factory.impl.vsphere;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.VSphereImageModel;
import io.maestro3.agent.model.base.PlatformType;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.agent.vsphere.dao.IVSphereImageRepository;
import io.maestro3.agent.vsphere.dao.IVSphereRegionRepository;
import io.maestro3.agent.vsphere.dao.IVSphereTenantRepository;
import io.maestro3.agent.vsphere.model.DeployType;
import io.maestro3.agent.vsphere.model.VSphere;
import io.maestro3.agent.vsphere.model.VSphereTenant;
import io.maestro3.agent.vsphere.model.image.GuestOS;
import io.maestro3.agent.vsphere.model.image.VsphereImage;
import io.maestro3.agent.vsphere.service.configuration.VSphereConfigurationWizardConstant;
import io.maestro3.sdk.internal.util.Assert;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkOptionItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableRowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class VSphereCreateImageCommand extends AbstractAdminCommand<VSphereImageModel> {

    private final IVSphereRegionRepository repository;
    private final IVSphereImageRepository imageRepository;
    private final IVSphereTenantRepository tenantRepository;

    @Autowired
    public VSphereCreateImageCommand(IVSphereTenantRepository tenantRepository,
                                     IVSphereRegionRepository repository,
                                     IVSphereImageRepository imageRepository) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.imageRepository = imageRepository;
    }

    @Override
    public VSphereImageModel getParams(String body, String... queryParams) {
        VSphereImageModel shapeConfigDto = JsonUtils.parseJson(body, VSphereImageModel.class);
        shapeConfigDto.setRegion(queryParams[0]);
        shapeConfigDto.setTenant(queryParams[1]);
        return shapeConfigDto;
    }

    @Override
    protected VSphereImageModel buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException("Single command generation is not supported");
    }

    @Override
    public List<VSphereImageModel> buildRequests(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, VSphereConfigurationWizardConstant.TENANT_NAME);
        String regionName = PrivateWizardUtils.getSelectedOptionItem(firstStep, VSphereConfigurationWizardConstant.REGION_NAME_ITEM).getTitle();
        VSphere region = repository.findByAliasInCloud(regionName);
        SdkOptionItem placementSelect = PrivateWizardUtils.getSelectedOptionItem(secondStep,
            VSphereConfigurationWizardConstant.PLACEMENT_SELECT_ITEM);
        SdkOptionItem deploySelect = PrivateWizardUtils.getSelectedOptionItem(placementSelect.getSelect(),
            VSphereConfigurationWizardConstant.DEPLOY_SELECT_ITEM);
        if (deploySelect.getValue().equals(DeployType.CONTENT_TEMPLATE.name())) {
            return processTemplates(tenantName, region, thirdStep);
        } else {
            return processIsoImages(tenantName, region, thirdStep);
        }
    }

    private List<VSphereImageModel> processTemplates(String tenant, VSphere region, SdkPrivateStep thirdStep) {
        List<VSphereImageModel> models = new ArrayList<>();
        SdkTableItem table = PrivateWizardUtils.findItem(thirdStep.getData().getTable(),
            VSphereConfigurationWizardConstant.ITEMS_TABLE_ITEM,
            SdkTableItem::getName);
        List<SdkTableRowItem> rows = PrivateWizardUtils.getSelectedRowForTable(table);
        for (SdkTableRowItem row : rows) {
            VSphereImageModel model = new VSphereImageModel();
            model.setName(row.getHiddenData().get("name"));
            model.setAlias(row.getHiddenData().get("name"));
            model.setItemId(row.getHiddenData().get("id"));
            model.setPlatformType(PlatformType.valueOf((String) row.getCell().get(2).getValue()));
            model.setTenant(tenant);
            model.setRegion(region.getRegionAlias());
            models.add(model);
        }
        return models;
    }

    private List<VSphereImageModel> processIsoImages(String tenant, VSphere region, SdkPrivateStep thirdStep) {
        List<VSphereImageModel> models = new ArrayList<>();
        SdkTableItem table = PrivateWizardUtils.findItem(thirdStep.getData().getTable(),
            VSphereConfigurationWizardConstant.ISO_IMAGE_TABLE_ITEM,
            SdkTableItem::getName);
        List<SdkTableRowItem> rows = table.getRow().stream().filter(row -> Boolean.FALSE.equals(row.getDeleted())).collect(Collectors.toList());
        for (SdkTableRowItem row : rows) {
            VSphereImageModel model = new VSphereImageModel();
            model.setName((String) row.getCell().get(0).getValue());
            model.setAlias((String) row.getCell().get(1).getValue());
            model.setGuestOS(GuestOS.valueOf((String) row.getCell().get(2).getValue()));
            model.setPlatformType(PlatformType.valueOf((String) row.getCell().get(3).getValue()));
            model.setTenant(tenant);
            model.setRegion(region.getRegionAlias());
            models.add(model);
        }
        return models;
    }

    @Override
    public AdminSdkResponse execute(VSphereImageModel imageModel) {
        VSphere region = repository.findByAliasInCloud(imageModel.getRegion());
        if (region == null) {
            throw new IllegalArgumentException("region not found by region alias " + imageModel.getRegion());
        }
        VSphereTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(imageModel.getTenant(), region.getId());
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant is not found by alias " + imageModel.getTenant());
        }
        VsphereImage existingImage = imageRepository.findImageForTenantInRegionByAlias(tenant.getId(), region.getId(), imageModel.getAlias());

        if (existingImage != null) {
            throw new IllegalArgumentException("Image with specified alias already exist in cloud " + imageModel.getRegion());
        }
        DeployType deployType = tenant.getDeployType();
        if (deployType == DeployType.CONTENT_TEMPLATE) {
            Assert.hasText(imageModel.getItemId(), "item_id");
        } else {
            Assert.hasText(imageModel.getName(), "name");
            Assert.notNull(imageModel.getGuestOS(), "guest_os");
        }
        VsphereImage image = new VsphereImage();
        image.setName(imageModel.getName());
        image.setAlias(imageModel.getAlias());
        image.setGuestOS(imageModel.getGuestOS());
        image.setItemId(imageModel.getItemId());
        image.setPlatformType(imageModel.getPlatformType());
        image.setTenantId(tenant.getId());
        image.setvSphereId(region.getId());
        imageRepository.save(image);
        return AdminSdkResponse.of("Image was successfully saved");
    }

    @Override
    public SdkAdminCommand prepareCommand(VSphereImageModel params) {
        Map<String, String> placeholders = new HashMap<>();
        String template = "m3admin private vsphere create_image " +
            "--region_alias ${REGION_NAME} " +
            "--tenant_alias ${TENANT_NAME} " +
            "--image_alias ${IMAGE_ALIAS} " +
            "--name ${NAME} " +
            "--platform ${PLATFORM} ";
        if (params.getItemId() != null) {
            placeholders.put("ITEM_ID", String.valueOf(params.getItemId()));
            template += "--item_id ${ITEM_ID} ";
        }
        if (params.getGuestOS() != null) {
            template += "--guest_os ${GUEST_OS} ";
            placeholders.put("GUEST_OS", params.getGuestOS().name());
        }
        placeholders.put("REGION_NAME", params.getRegion());
        placeholders.put("TENANT_NAME", String.valueOf(params.getTenant()));
        placeholders.put("IMAGE_ALIAS", String.valueOf(params.getAlias()));
        placeholders.put("NAME", String.valueOf(params.getName()));
        placeholders.put("PLATFORM", params.getPlatformType().name());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.VSPHERE_CREATE_IMAGE;
    }

}
