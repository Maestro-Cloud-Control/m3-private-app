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

package io.maestro3.agent.admin.factory.impl.openstack;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.ImageDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.base.PlatformType;
import io.maestro3.agent.model.compute.ImageVisibility;
import io.maestro3.agent.model.image.OpenStackMachineImage;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.service.DbServicesProvider;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkCellItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableRowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class OpenstackCreateImageCommand extends AbstractAdminCommand<ImageDto> {

    private final DbServicesProvider dbServicesProvider;
    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackCreateImageCommand(DbServicesProvider dbServicesProvider,
                                       IOpenStackRegionRepository regionService) {
        this.dbServicesProvider = dbServicesProvider;
        this.regionService = regionService;
    }

    @Override
    public ImageDto getParams(String body, String... queryParams) {
        ImageDto image = JsonUtils.parseJson(body, ImageDto.class);
        image.setRegionAlias(queryParams[0]);
        return image;
    }


    @Override
    public ImageDto buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException("Single command generation is not supported");
    }

    @Override
    protected List<ImageDto> buildRequests(SdkPrivateWizard wizard) {
        SdkTableItem imageTable = PrivateWizardUtils.findItem(PrivateWizardUtils.getStepById(5, wizard.getStep()).getData().getTable(), OpenstackConfigurationWizardConstant.IMAGE_TO_PLATFORM_TABLE, SdkTableItem::getName);
        List<SdkTableRowItem> rows = imageTable.getRow();
        List<ImageDto> requests = new ArrayList<>();
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        String regionName = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.REGION_NAME_ITEM);
        for (SdkTableRowItem row : rows) {
            String imageNativeName = row.getHiddenData().get("name");
            String imageNativeId = row.getHiddenData().get("id");
            List<SdkCellItem> cells = row.getCell();
            SdkCellItem platformCell = cells.get(cells.size() - 1);
            String platform = (String) platformCell.getValue();
            requests.add(new ImageDto(imageNativeName, PlatformType.valueOf(platform), regionName, ImageVisibility.PUBLIC,
                "Available", imageNativeId, imageNativeName));
        }
        return requests;
    }

    @Override
    public AdminSdkResponse execute(ImageDto image) {
        String regionAlias = image.getRegionAlias();
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        EntityValidator.validate(image);
        OpenStackMachineImage existingImage = dbServicesProvider.getMachineImageDbService().findByNativeId(image.getNativeId());
        if (existingImage != null && existingImage.getRegionId().equals(existingRegion.getId())) {
            throw new IllegalStateException("ERROR: Image specified nativeId already exist. Received id" + image.getNativeId());
        }
        OpenStackMachineImage openStackMachineImage = image.toOsImage(existingRegion.getId());
        dbServicesProvider.getMachineImageDbService().save(openStackMachineImage);
        return AdminSdkResponse.of("Image was successfully added to region " + regionAlias);
    }

    @Override
    public SdkAdminCommand prepareCommand(ImageDto params) {
        String template = "m3admin private openstack create_image --region_alias ${REGION_ALIAS} --name_alias ${NAME_ALIAS} " +
            "--platform_type ${PLATFORM_TYPE} --image_visibility ${IMAGE_VISIBILITY} --image_status ${IMAGE_STATUS} " +
            "--native_id ${NATIVE_ID} --native_name ${NATIVE_NAME}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("NAME_ALIAS", params.getNameAlias());
        placeholders.put("PLATFORM_TYPE", params.getPlatformType().name());
        placeholders.put("IMAGE_VISIBILITY", params.getImageVisibility().name());
        placeholders.put("IMAGE_STATUS", params.getImageStatus());
        placeholders.put("NATIVE_ID", params.getNativeId());
        placeholders.put("NATIVE_NAME", params.getNativeName());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CREATE_IMAGE;
    }
}
