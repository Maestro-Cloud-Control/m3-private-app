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

package io.maestro3.agent.admin.model;

import io.maestro3.agent.nutanix.model.NutanixShape;


public class NutanixShapeConfigDto extends BaseRegionDto {

    private String name;
    private int cpuCount;
    private int memory;
    private int storageGb;

    public NutanixShapeConfigDto() {
    }

    public NutanixShapeConfigDto(String name, int cpuCount, int memory, int storageGb) {
        this.name = name;
        this.cpuCount = cpuCount;
        this.memory = memory;
        this.storageGb = storageGb;
    }

    public NutanixShape toShape() {
        NutanixShape shape = new NutanixShape();
        shape.setCpuCount(this.getCpuCount());
        shape.setNameAlias(this.getName());
        shape.setMemorySizeMb(this.getMemory());
        shape.setDiskSizeMb(this.getStorageGb());
        return shape;
    }


    public void setCpuCount(int cpuCount) {
        this.cpuCount = cpuCount;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public void setStorageGb(int storageGb) {
        this.storageGb = storageGb;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getCpuCount() {
        return cpuCount;
    }

    public int getMemory() {
        return memory;
    }

    public int getStorageGb() {
        return storageGb;
    }
}
