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
import io.maestro3.agent.admin.model.ShapeConfigDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.flavor.OpenStackFlavorConfig;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableRowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class OpenstackCreateShapeCommand extends AbstractAdminCommand<ShapeConfigDto> {

    private final int defaultStorageSizeForUnknownFlavors;

    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackCreateShapeCommand(IOpenStackRegionRepository regionService,
                                       @Value("${configuration.wizard.os.flavor.default.disk}") int defaultStorageSizeForUnknownFlavors) {
        this.regionService = regionService;
        this.defaultStorageSizeForUnknownFlavors = defaultStorageSizeForUnknownFlavors;
    }

    @Override
    public ShapeConfigDto getParams(String body, String... queryParams) {
        ShapeConfigDto shapeConfigDto = JsonUtils.parseJson(body, ShapeConfigDto.class);
        shapeConfigDto.setRegionAlias(queryParams[0]);
        return shapeConfigDto;
    }

    @Override
    public ShapeConfigDto buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException("Single command generation is not supported");
    }

    @Override
    protected List<ShapeConfigDto> buildRequests(SdkPrivateWizard wizard) {
        List<ShapeConfigDto> requests = new ArrayList<>();
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        String regionName = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.REGION_NAME_ITEM);
        SdkTableItem flavorsTable = PrivateWizardUtils.findItem(PrivateWizardUtils.getStepById(3, wizard.getStep()).getData().getTable(),
            OpenstackConfigurationWizardConstant.FLAVOR_TABLE, SdkTableItem::getName);
        List<SdkTableRowItem> selectedFlavors = PrivateWizardUtils.getSelectedRowForTable(flavorsTable);
        for (SdkTableRowItem row : selectedFlavors) {
            Map<String, String> hiddenData = row.getHiddenData();
            String id = hiddenData.get("id");
            String name = hiddenData.get("name");
            String ram = hiddenData.get("ram");
            String cpu = hiddenData.get("cpu");
            String disk = hiddenData.get("disk");
            int diskSizeMb = Integer.parseInt(disk);
            requests.add(new ShapeConfigDto(id, name, name, Integer.parseInt(cpu), diskSizeMb == 0 ? defaultStorageSizeForUnknownFlavors : diskSizeMb,
                Integer.parseInt(ram), "UNKNOWN", regionName));
        }
        return requests;
    }

    @Override
    public AdminSdkResponse execute(ShapeConfigDto shapeConfigDto) {
        String regionAlias = shapeConfigDto.getRegionAlias();
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        EntityValidator.validate(shapeConfigDto);
        List<OpenStackFlavorConfig> allowedShapes = existingRegion.getAllowedShapes();
        OpenStackFlavorConfig existingShape = null;
        for (OpenStackFlavorConfig allowedShape : allowedShapes) {
            if (allowedShape.getNativeId().equals(shapeConfigDto.getNativeId())) {
                existingShape = allowedShape;
                break;
            }
        }
        if (existingShape != null) {
            throw new IllegalStateException("ERROR: Shape with specified nativeId already exist. Received id" + shapeConfigDto.getNativeId());
        }
        OpenStackFlavorConfig config = shapeConfigDto.toConfig();
        allowedShapes.add(config);
        regionService.save(existingRegion);
        return AdminSdkResponse.of("Shape was successfully added to region " + regionAlias);
    }

    @Override
    public SdkAdminCommand prepareCommand(ShapeConfigDto params) {
        String template = "m3admin private openstack create_shape --region_alias ${REGION_ALIAS}" +
            " --native_id ${NATIVE_ID} --name_alias ${NAME_ALIAS}" +
            " --cpu ${CPU} --memory ${MEMORY} --storage ${STORAGE}" +
            " --native_name ${NATIVE_NAME} --processor_type ${PROCESSOR_TYPE}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("NATIVE_ID", params.getNativeId());
        placeholders.put("NAME_ALIAS", params.getNameAlias());
        placeholders.put("CPU", Integer.toString(params.getCpuCount()));
        placeholders.put("MEMORY", Long.toString(params.getMemorySizeMb()));
        placeholders.put("STORAGE", Long.toString(params.getDiskSizeMb()));
        placeholders.put("NATIVE_NAME", params.getNativeName());
        placeholders.put("PROCESSOR_TYPE", params.getProcessorType());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CREATE_SHAPE;
    }
}
