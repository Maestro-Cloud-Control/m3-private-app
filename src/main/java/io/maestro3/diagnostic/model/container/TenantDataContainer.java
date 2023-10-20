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

package io.maestro3.diagnostic.model.container;

import io.maestro3.agent.model.base.PrivateCloudType;
import io.maestro3.agent.model.base.ShapeConfig;
import io.maestro3.agent.model.base.TenantState;
import io.maestro3.sdk.internal.util.CollectionUtils;

import java.util.Collection;
import java.util.stream.Collectors;


public class TenantDataContainer {
    private String tenantAlias;
    private String name;
    private String regionAlias;
    private TenantState state;
    private boolean managementAvailable;
    private String shapes;
    private String lastStatusUpdate;
    private PrivateCloudType cloud;
    private String nativeName;
    private String securityGroupName;
    private String userdataTemplateId;

    public String getNativeName() {
        return nativeName;
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    public String getUserdataTemplateId() {
        return userdataTemplateId;
    }

    public void setUserdataTemplateId(String userdataTemplateId) {
        this.userdataTemplateId = userdataTemplateId;
    }

    public String getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    public void setLastStatusUpdate(String lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }

    public String getTenantAlias() {
        return tenantAlias;
    }

    public void setTenantAlias(String tenantAlias) {
        this.tenantAlias = tenantAlias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegionAlias() {
        return regionAlias;
    }

    public void setRegionAlias(String regionAlias) {
        this.regionAlias = regionAlias;
    }

    public TenantState getState() {
        return state;
    }

    public void setState(TenantState state) {
        this.state = state;
    }

    public String getShapes() {
        return shapes;
    }

    public void setShapes(String shapes) {
        this.shapes = shapes;
    }

    public PrivateCloudType getCloud() {
        return cloud;
    }

    public void setCloud(PrivateCloudType cloud) {
        this.cloud = cloud;
    }

    public boolean isManagementAvailable() {
        return managementAvailable;
    }

    public void setManagementAvailable(boolean managementAvailable) {
        this.managementAvailable = managementAvailable;
    }

    public static TenantDataContainerBuilder builder() {
        return new TenantDataContainerBuilder();
    }

    public static final class TenantDataContainerBuilder {
        private String tenantAlias;
        private String name;
        private String regionAlias;
        private PrivateCloudType cloud;
        private String shapes;
        private TenantState state;
        private boolean managementAvailable;
        private String lastStatusUpdate;
        private String nativeName;
        private String securityGroupName;
        private String userdataTemplateId;

        private TenantDataContainerBuilder() {}

        public TenantDataContainerBuilder withTenantAlias(String tenantAlias) {
            this.tenantAlias = tenantAlias;
            return this;
        }

        public TenantDataContainerBuilder withNativeName(String nativeName) {
            this.nativeName = nativeName;
            return this;
        }

        public TenantDataContainerBuilder withSecurityGroupName(String securityGroupName) {
            this.securityGroupName = securityGroupName;
            return this;
        }

        public TenantDataContainerBuilder withUserdataTemplateId(String userdataTemplateId) {
            this.userdataTemplateId = userdataTemplateId;
            return this;
        }

        public TenantDataContainerBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public TenantDataContainerBuilder withRegionAlias(String regionAlias) {
            this.regionAlias = regionAlias;
            return this;
        }

        public TenantDataContainerBuilder withShapes(Collection<ShapeConfig> shapes) {
            this.shapes = CollectionUtils.isNotEmpty(shapes)
                    ? shapes
                    .stream()
                    .map(ShapeConfig::getNameAlias)
                    .collect(Collectors.joining(", "))
                    : "Not specified";
            return this;
        }

        public TenantDataContainerBuilder withState(TenantState state) {
            this.state = state;
            return this;
        }

        public TenantDataContainerBuilder withManagementAvailable(boolean managementAvailable) {
            this.managementAvailable = managementAvailable;
            return this;
        }

        public TenantDataContainerBuilder withCloud(PrivateCloudType cloud) {
            this.cloud = cloud;
            return this;
        }

        public TenantDataContainerBuilder withLastStatusUpdate(String lastStatusUpdate) {
            this.lastStatusUpdate = lastStatusUpdate;
            return this;
        }

        public TenantDataContainer build() {
            TenantDataContainer tenantDataContainer = new TenantDataContainer();
            tenantDataContainer.setTenantAlias(tenantAlias);
            tenantDataContainer.setName(name);
            tenantDataContainer.setRegionAlias(regionAlias);
            tenantDataContainer.setState(state);
            tenantDataContainer.setCloud(cloud);
            tenantDataContainer.setShapes(shapes);
            tenantDataContainer.setManagementAvailable(managementAvailable);
            tenantDataContainer.setLastStatusUpdate(lastStatusUpdate);
            tenantDataContainer.setNativeName(nativeName);
            tenantDataContainer.setSecurityGroupName(securityGroupName);
            tenantDataContainer.setUserdataTemplateId(userdataTemplateId);
            return tenantDataContainer;
        }
    }
}
