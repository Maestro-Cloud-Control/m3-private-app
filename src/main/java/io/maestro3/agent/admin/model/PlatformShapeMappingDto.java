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

import io.maestro3.agent.model.PlatformShapeMapping;


public class PlatformShapeMappingDto extends BaseRegionDto {

    private String imageName;
    private String platformType;
    private double minMemoryGb;
    private int minStorageSizeGb;

    public PlatformShapeMappingDto() {
    }

    public PlatformShapeMappingDto(String imageName, String platformType, String region) {
        this.imageName = imageName;
        this.platformType = platformType;
        this.setRegionAlias(region);
    }

    public PlatformShapeMappingDto(PlatformShapeMapping shapeMapping) {
        this.platformType = shapeMapping.getPlatformType();
        this.minMemoryGb = shapeMapping.getMinMemoryGb();
        this.minStorageSizeGb = shapeMapping.getMinStorageSizeGb();
        this.imageName = shapeMapping.getName();
    }

    public PlatformShapeMapping toShapeMapping() {
        PlatformShapeMapping mapping = new PlatformShapeMapping();
        mapping.setMinMemoryGb(this.minMemoryGb);
        mapping.setName(this.getImageName());
        mapping.setMinStorageSizeGb(this.minStorageSizeGb);
        mapping.setPlatformType(this.platformType);
        return mapping;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(String platformType) {
        this.platformType = platformType;
    }

    public double getMinMemoryGb() {
        return minMemoryGb;
    }

    public void setMinMemoryGb(double minMemoryGb) {
        this.minMemoryGb = minMemoryGb;
    }

    public int getMinStorageSizeGb() {
        return minStorageSizeGb;
    }

    public void setMinStorageSizeGb(int minStorageSizeGb) {
        this.minStorageSizeGb = minStorageSizeGb;
    }

    @Override
    public String toString() {
        return "PlatformShapeMapping{" +
                "platformType='" + platformType + '\'' +
                ", minMemoryGb=" + minMemoryGb +
                ", minStorageSizeGb=" + minStorageSizeGb +
                '}';
    }
}
