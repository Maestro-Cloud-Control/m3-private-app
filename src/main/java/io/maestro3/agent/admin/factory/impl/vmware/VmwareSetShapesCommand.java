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

package io.maestro3.agent.admin.factory.impl.vmware;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.VmwareShapeConfigDto;
import io.maestro3.agent.dao.IVmwareRegionRepository;
import io.maestro3.agent.model.VmwareShape;
import io.maestro3.agent.model.cloud.VCloud;
import io.maestro3.agent.service.configuration.VmwareConfigurationWizardConstant;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class VmwareSetShapesCommand extends AbstractAdminCommand<VmwareShapeConfigDto> {

    private final IVmwareRegionRepository repository;

    @Autowired
    public VmwareSetShapesCommand(IVmwareRegionRepository repository) {
        this.repository = repository;
    }

    @Override
    public VmwareShapeConfigDto getParams(String body, String... queryParams) {
        VmwareShapeConfigDto shapeConfigDto = JsonUtils.parseJson(body, VmwareShapeConfigDto.class);
        shapeConfigDto.setRegionAlias(queryParams[0]);
        return shapeConfigDto;
    }

    @Override
    protected VmwareShapeConfigDto buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException("Single command generation is not supported");
    }

    @Override
    public List<VmwareShapeConfigDto> buildRequests(SdkPrivateWizard wizard) {
        List<VmwareShapeConfigDto> shapes = new ArrayList<>();
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        String regionName = PrivateWizardUtils.getTextValue(secondStep, VmwareConfigurationWizardConstant.REGION_NAME_ITEM);
        SdkTableItem shapesTable = PrivateWizardUtils.findItem(thirdStep.getData().getTable(),
            VmwareConfigurationWizardConstant.SHAPES_TABLE_ITEM, SdkTableItem::getName);
        List<SdkTableRowItem> rows = shapesTable.getRow().stream()
            .filter(row -> row.getDeleted() == null || !row.getDeleted())
            .collect(Collectors.toList());
        for (SdkTableRowItem row : rows) {
            List<SdkCellItem> cells = row.getCell();
            VmwareShapeConfigDto shape = new VmwareShapeConfigDto(
                (String) cells.get(0).getValue(), 0,
                Integer.parseInt((String) cells.get(1).getValue()),
                Integer.parseInt((String) cells.get(2).getValue()),
                Integer.parseInt((String) cells.get(3).getValue()));
            shape.setRegionAlias(regionName);
            shapes.add(shape);
        }
        return shapes;
    }

    @Override
    public AdminSdkResponse execute(VmwareShapeConfigDto shape) {
        VCloud cloud = repository.findByAliasInCloud(shape.getRegionAlias());
        if (cloud == null) {
            throw new IllegalStateException("ERROR: vCloud is not exist with region alias  " + shape.getRegionAlias());
        }
        StringBuilder shapeCheckResult = new StringBuilder();
        if (shape.getCpuCount() < 1) {
            shapeCheckResult.append("ERROR: " + shape.getName() + " should contains more then 1 cpuCount\n");
        }
        if (shape.getMemory() < 512) {
            shapeCheckResult.append("ERROR: " + shape.getName() + " should contains more then 512 memory\n");
        }
        if (shapeCheckResult.length() > 0) {
            throw new IllegalArgumentException(shapeCheckResult.toString());
        }
        VmwareShape existingShape = cloud.resolveShapeByName(shape.getName());
        String msg;
        if (existingShape != null) {
            existingShape.setCpuCount(shape.getCpuCount());
            existingShape.setMemorySizeMb(shape.getMemory());
            existingShape.setDiskSizeMb(shape.getStorageGb());
            existingShape.setUnits(shape.getUnits());
            msg = "Existing shape was updated";
        } else {
            cloud.getAllowedShapes().add(shape.toShape());
            msg = "Shape were successfully configured";
        }
        repository.save(cloud);
        return AdminSdkResponse.of(msg);
    }

    @Override
    public SdkAdminCommand prepareCommand(VmwareShapeConfigDto params) {
        String template = "m3admin private vmware set_tenant_shape " +
            "--region_alias ${REGION_NAME} " +
            "--name ${SHAPE_NAME} " +
            "--cpu ${CPU} " +
            "--memory ${RAM} " +
            "--storage ${STORAGE}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_NAME", params.getRegionAlias());
        placeholders.put("SHAPE_NAME", params.getName());
        placeholders.put("CPU", String.valueOf(params.getCpuCount()));
        placeholders.put("RAM", String.valueOf(params.getMemory()));
        placeholders.put("STORAGE", String.valueOf(params.getStorageGb()));
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.VMWARE_CONFIGURE_SHAPES;
    }

}
