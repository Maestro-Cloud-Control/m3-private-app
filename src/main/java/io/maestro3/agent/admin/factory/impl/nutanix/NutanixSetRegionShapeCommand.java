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

package io.maestro3.agent.admin.factory.impl.nutanix;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.IAdminCommand;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.NutanixImageModel;
import io.maestro3.agent.admin.model.NutanixShapeConfigDto;
import io.maestro3.agent.nutanix.dao.INutanixRegionRepository;
import io.maestro3.agent.nutanix.model.NutanixRegion;
import io.maestro3.agent.nutanix.model.NutanixShape;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class NutanixSetRegionShapeCommand extends AbstractAdminCommand<NutanixShapeConfigDto> {

    private final INutanixRegionRepository regionRepository;

    @Autowired
    public NutanixSetRegionShapeCommand(INutanixRegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    public NutanixShapeConfigDto getParams(String body, String... queryParams) {
        NutanixShapeConfigDto shape = JsonUtils.parseJson(body, NutanixShapeConfigDto.class);
        shape.setRegionAlias(queryParams[0]);
        return shape;
    }

    @Override
    protected NutanixShapeConfigDto buildRequest(SdkPrivateWizard wizard) {
        return null;
    }

    @Override
    public AdminSdkResponse execute(NutanixShapeConfigDto shape) {
        String regionAlias = shape.getRegionAlias();
        NutanixRegion region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Nutanix region is not exist with region alias " + regionAlias);
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
        NutanixShape existingShape = region.resolveShapeByName(shape.getName());
        String msg;
        if (existingShape != null) {
            existingShape.setCpuCount(shape.getCpuCount());
            existingShape.setMemorySizeMb(shape.getMemory());
            existingShape.setDiskSizeMb(shape.getStorageGb());
            msg = "Existing shape was updated";
        } else {
            region.getAllowedShapes().add(shape.toShape());
            msg = "Shape were successfully configured";
        }
        regionRepository.save(region);
        return AdminSdkResponse.of(msg);
    }

    @Override
    public SdkAdminCommand prepareCommand(NutanixShapeConfigDto params) {
        String template = "m3admin private nutanix set_region_shape --region_alias ${REGION_ALIAS} --name ${NAME}" +
            " --cpu ${CPU} --memory ${MEMORY} --storage ${STORAGE}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("NAME", params.getName());
        placeholders.put("CPU", Integer.toString(params.getCpuCount()));
        placeholders.put("MEMORY", Integer.toString(params.getMemory()));
        placeholders.put("STORAGE", Integer.toString(params.getStorageGb()));
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.NUTANIX_SET_REGION_SHAPE;
    }
}
