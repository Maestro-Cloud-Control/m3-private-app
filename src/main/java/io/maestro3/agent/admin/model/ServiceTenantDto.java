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

import io.maestro3.agent.model.region.OpenStackServiceTenantInfo;


public class ServiceTenantDto extends BaseRegionDto {
    private String name;
    private String nativeId;

    public ServiceTenantDto() {
    }

    public ServiceTenantDto(OpenStackServiceTenantInfo serviceTenantInfo) {
        this.name = serviceTenantInfo.getName();
        this.nativeId = serviceTenantInfo.getNativeId();
    }

    public OpenStackServiceTenantInfo toServiceTenantInfo() {
        OpenStackServiceTenantInfo info = new OpenStackServiceTenantInfo();
        info.setName(this.name);
        info.setNativeId(this.nativeId);
        return info;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNativeId() {
        return nativeId;
    }

    public void setNativeId(String nativeId) {
        this.nativeId = nativeId;
    }
}
