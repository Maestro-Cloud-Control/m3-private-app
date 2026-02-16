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

import io.maestro3.agent.vsphere.model.VSphereShape;


public class VSphereShapeConfigDto {

    private String name;
    private double units;
    private int cpuCount;
    private int memory;
    private int storageGb;
    private String regionAlias;

    public VSphereShapeConfigDto() {
    }

    public VSphereShapeConfigDto(String name, double units, int cpuCount, int memory, int storageGb) {
        this.name = name;
        this.units = units;
        this.cpuCount = cpuCount;
        this.memory = memory;
        this.storageGb = storageGb;
    }

    public VSphereShape toShape(){
        VSphereShape shape = new VSphereShape();
        shape.setCpuCount(this.getCpuCount());
        shape.setNameAlias(this.getName());
        shape.setMemorySizeMb(this.getMemory());
        shape.setDiskSizeMb(this.getStorageGb());
        shape.setUnits(this.getUnits());
        return shape;
    }

    public String getRegionAlias() {
        return regionAlias;
    }

    public void setRegionAlias(String regionAlias) {
        this.regionAlias = regionAlias;
    }

    public void setUnits(double units) {
        this.units = units;
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

    public double getUnits() {
        return units;
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
