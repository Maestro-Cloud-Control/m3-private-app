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

import io.maestro3.agent.model.compute.DiskConfig;
import io.maestro3.agent.model.enums.OpenStackVersion;
import io.maestro3.agent.model.region.OpenStackRegionConfig;


public class RegionConfigDto {
    private String nameAlias;
    private int regionNumber;
    private String serverNamePrefix;
    private boolean enableScheduledDescribers;
    private String keystoneAuthUrl;
    private String nativeRegionName;
    private DiskConfig serverDiskConfig;
    private OpenStackVersion osVersion;

    public RegionConfigDto() {
    }

    public RegionConfigDto(OpenStackRegionConfig regionConfig) {
        this.nameAlias = regionConfig.getRegionAlias();
        this.regionNumber = regionConfig.getRegionNumber();
        this.serverNamePrefix = regionConfig.getServerNamePrefix();
        this.enableScheduledDescribers = regionConfig.isEnableScheduledDescribers();
        this.keystoneAuthUrl = regionConfig.getKeystoneAuthUrl();
        this.nativeRegionName = regionConfig.getNativeRegionName();
        this.serverDiskConfig = regionConfig.getServerDiskConfig();
        this.osVersion = regionConfig.getOsVersion();
    }

    public OpenStackRegionConfig toRegionConfig() {
        OpenStackRegionConfig regionConfig = new OpenStackRegionConfig();
        regionConfig.setRegionAlias(this.nameAlias);
        regionConfig.setRegionNumber(this.regionNumber);
        regionConfig.setOsVersion(this.osVersion);
        regionConfig.setServerNamePrefix(this.serverNamePrefix);
        regionConfig.setEnableScheduledDescribers(this.enableScheduledDescribers);
        regionConfig.setKeystoneAuthUrl(this.keystoneAuthUrl);
        regionConfig.setNativeRegionName(this.nativeRegionName);
        regionConfig.setServerDiskConfig(this.serverDiskConfig);
        return regionConfig;
    }

    public OpenStackVersion getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(OpenStackVersion osVersion) {
        this.osVersion = osVersion;
    }

    public String getNameAlias() {
        return nameAlias;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
    }

    public int getRegionNumber() {
        return regionNumber;
    }

    public void setRegionNumber(int regionNumber) {
        this.regionNumber = regionNumber;
    }

    public String getServerNamePrefix() {
        return serverNamePrefix;
    }

    public void setServerNamePrefix(String serverNamePrefix) {
        this.serverNamePrefix = serverNamePrefix;
    }

    public boolean isEnableScheduledDescribers() {
        return enableScheduledDescribers;
    }

    public void setEnableScheduledDescribers(boolean enableScheduledDescribers) {
        this.enableScheduledDescribers = enableScheduledDescribers;
    }

    public String getKeystoneAuthUrl() {
        return keystoneAuthUrl;
    }

    public void setKeystoneAuthUrl(String keystoneAuthUrl) {
        this.keystoneAuthUrl = keystoneAuthUrl;
    }

    public String getNativeRegionName() {
        return nativeRegionName;
    }

    public void setNativeRegionName(String nativeRegionName) {
        this.nativeRegionName = nativeRegionName;
    }

    public DiskConfig getServerDiskConfig() {
        return serverDiskConfig;
    }

    public void setServerDiskConfig(DiskConfig serverDiskConfig) {
        this.serverDiskConfig = serverDiskConfig;
    }
}
