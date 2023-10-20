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

import io.maestro3.agent.model.tenant.OpenStackTenant;


public class TenantDto extends BaseRegionDto {
    private String nativeId;
    private String networkId;
    private String nativeName;
    private String securityGroupId;
    private String securityGroupName;
    private String nameAlias;
    private String domainName;
    private boolean managementAvailable;

    public TenantDto() {
    }

    public TenantDto(String nativeId, String networkId, String nativeName, String securityGroupId, String securityGroupName,
                     String nameAlias, String domainName, String region) {
        this.nativeId = nativeId;
        this.networkId = networkId;
        this.nativeName = nativeName;
        this.securityGroupId = securityGroupId;
        this.securityGroupName = securityGroupName;
        this.nameAlias = nameAlias;
        this.domainName = domainName;
        this.setRegionAlias(region);
    }

    public TenantDto(OpenStackTenant tenantConfig) {
        this.nativeId = tenantConfig.getNativeId();
        this.networkId = tenantConfig.getNetworkId();
        this.nativeName = tenantConfig.getNativeName();
        this.securityGroupId = tenantConfig.getSecurityGroupId();
        this.securityGroupName = tenantConfig.getSecurityGroupName();
        this.nameAlias = tenantConfig.getTenantAlias();
        this.domainName = tenantConfig.getDomainName();
    }

    public OpenStackTenant toTenantConfig(String regionId) {
        OpenStackTenant config = new OpenStackTenant();
        config.setNativeId(nativeId);
        config.setRegionId(regionId);
        config.setNetworkId(networkId);
        config.setNativeName(nativeName);
        config.setSecurityGroupName(securityGroupName);
        config.setSecurityGroupId(securityGroupId);
        config.setTenantAlias(nameAlias);
        config.setDomainName(domainName);
        return config;
    }

    public boolean isManagementAvailable() {
        return managementAvailable;
    }

    public void setManagementAvailable(boolean managementAvailable) {
        this.managementAvailable = managementAvailable;
    }

    public String getNativeId() {
        return nativeId;
    }

    public void setNativeId(String nativeId) {
        this.nativeId = nativeId;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    public String getNameAlias() {
        return nameAlias;
    }

    public void setNameAlias(String nameAlias) {
        this.nameAlias = nameAlias;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
