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

import io.maestro3.agent.model.flavor.OpenStackFlavorConfig;



public class ShapeConfigDto extends BaseRegionDto {
    private String nativeId;
    private String nativeName;
    private String nameAlias;
    private int cpuCount;
    private long diskSizeMb;
    private long memorySizeMb;
    private String processorType;

    public ShapeConfigDto() {
    }

    public ShapeConfigDto(String nativeId, String nativeName, String nameAlias, int cpuCount, long diskSizeMb, long memorySizeMb, String processorType, String region) {
        this.nativeId = nativeId;
        this.nativeName = nativeName;
        this.nameAlias = nameAlias;
        this.cpuCount = cpuCount;
        this.diskSizeMb = diskSizeMb;
        this.memorySizeMb = memorySizeMb;
        this.processorType = processorType;
        this.setRegionAlias(region);
    }

    public ShapeConfigDto(OpenStackFlavorConfig config) {
        this.nativeId = config.getNativeId();
        this.nativeName = config.getNativeName();
        this.nameAlias = config.getNameAlias();
        this.cpuCount = config.getCpuCount();
        this.diskSizeMb = config.getDiskSizeMb();
        this.memorySizeMb = config.getMemorySizeMb();
        this.processorType = config.getProcessorType();
    }

    public OpenStackFlavorConfig toConfig() {
        OpenStackFlavorConfig config = new OpenStackFlavorConfig();
        config.setNativeId(nativeId);
        config.setNativeName(nativeName);
        config.setNameAlias(nameAlias);
        config.setCpuCount(cpuCount);
        config.setDiskSizeMb(diskSizeMb);
        config.setMemorySizeMb(memorySizeMb);
        config.setProcessorType(processorType);
        return config;
    }

    public String getNativeId() {
        return nativeId;
    }

    public void setNativeId(String nativeId) {
        this.nativeId = nativeId;
    }

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getNameAlias() {
        return nameAlias;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
    }

    public int getCpuCount() {
        return cpuCount;
    }

    public void setCpuCount(int cpuCount) {
        this.cpuCount = cpuCount;
    }

    public long getDiskSizeMb() {
        return diskSizeMb;
    }

    public void setDiskSizeMb(long diskSizeMb) {
        this.diskSizeMb = diskSizeMb;
    }

    public long getMemorySizeMb() {
        return memorySizeMb;
    }

    public void setMemorySizeMb(long memorySizeMb) {
        this.memorySizeMb = memorySizeMb;
    }

    public String getProcessorType() {
        return processorType;
    }

    public void setProcessorType(String processorType) {
        this.processorType = processorType;
    }
}
