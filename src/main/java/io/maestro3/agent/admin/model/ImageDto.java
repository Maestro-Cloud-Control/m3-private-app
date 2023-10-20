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

import io.maestro3.agent.model.compute.ImageVisibility;
import io.maestro3.agent.model.base.PlatformType;
import io.maestro3.agent.model.image.OpenStackMachineImage;


public class ImageDto {
    private String nameAlias;
    private PlatformType platformType;
    private String regionAlias;
    private double requiredMinMemoryGb;
    private int requiredMinStorageSizeGb;
    private ImageVisibility imageVisibility;
    private String imageStatus;
    private String nativeId;
    private String nativeName;

    public ImageDto() {
    }

    public ImageDto(String nameAlias, PlatformType platformType, String regionAlias, ImageVisibility imageVisibility,
                    String imageStatus, String nativeId, String nativeName) {
        this.nameAlias = nameAlias;
        this.platformType = platformType;
        this.regionAlias = regionAlias;
        this.imageVisibility = imageVisibility;
        this.imageStatus = imageStatus;
        this.nativeId = nativeId;
        this.nativeName = nativeName;
    }

    public ImageDto(OpenStackMachineImage image, String regionAlias) {
        this.nameAlias = image.getNameAlias();
        this.platformType = image.getPlatformType();
        this.regionAlias = regionAlias;
        this.requiredMinMemoryGb = image.getRequiredMinMemoryGb();
        this.requiredMinStorageSizeGb = image.getRequiredMinStorageSizeGb();
        this.imageVisibility = image.getImageVisibility();
        this.imageStatus = image.getImageStatus();
        this.nativeId = image.getNativeId();
        this.nativeName = image.getNativeName();
    }

    public OpenStackMachineImage toOsImage(String regionId) {
        OpenStackMachineImage image = new OpenStackMachineImage();
        image.setNameAlias(this.nameAlias);
        image.setPlatformType(this.platformType);
        image.setRegionId(regionId);
        image.setRequiredMinMemoryGb(this.requiredMinMemoryGb);
        image.setRequiredMinStorageSizeGb(this.requiredMinStorageSizeGb);
        image.setImageVisibility(this.imageVisibility);
        image.setImageStatus(this.imageStatus);
        image.setNativeId(this.nativeId);
        image.setNativeName(this.nativeName);
        return image;
    }

    public String getNameAlias() {
        return nameAlias;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }

    public String getRegionAlias() {
        return regionAlias;
    }

    public void setRegionAlias(String regionAlias) {
        this.regionAlias = regionAlias;
    }

    public double getRequiredMinMemoryGb() {
        return requiredMinMemoryGb;
    }

    public void setRequiredMinMemoryGb(double requiredMinMemoryGb) {
        this.requiredMinMemoryGb = requiredMinMemoryGb;
    }

    public int getRequiredMinStorageSizeGb() {
        return requiredMinStorageSizeGb;
    }

    public void setRequiredMinStorageSizeGb(int requiredMinStorageSizeGb) {
        this.requiredMinStorageSizeGb = requiredMinStorageSizeGb;
    }

    public ImageVisibility getImageVisibility() {
        return imageVisibility;
    }

    public void setImageVisibility(ImageVisibility imageVisibility) {
        this.imageVisibility = imageVisibility;
    }

    public String getImageStatus() {
        return imageStatus;
    }

    public void setImageStatus(String imageStatus) {
        this.imageStatus = imageStatus;
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
}
