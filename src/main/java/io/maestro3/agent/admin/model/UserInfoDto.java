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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.maestro3.agent.model.OpenStackUserInfo;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoDto extends BaseTenantDto {
    private String nativeId;
    private String name;
    private String password;
    private String domainName;

    public UserInfoDto() {
    }

    public UserInfoDto(String nativeId, String name, String password, String domainName, String tenant, String region) {
        this.nativeId = nativeId;
        this.name = name;
        this.password = password;
        this.domainName = domainName;
        this.setTenantAlias(tenant);
        this.setRegionAlias(region);
    }

    public UserInfoDto(OpenStackUserInfo osInfo) {
        this.nativeId = osInfo.getNativeId();
        this.name = osInfo.getName();
        this.domainName = osInfo.getDomainName();
    }

    public OpenStackUserInfo toUserInfo() {
        OpenStackUserInfo info = new OpenStackUserInfo();
        info.setName(this.name);
        info.setNativeId(this.nativeId);
        info.setPassword(this.password);
        info.setDomainName(this.domainName);
        return info;
    }

    public String getNativeId() {
        return nativeId;
    }

    public void setNativeId(String nativeId) {
        this.nativeId = nativeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
