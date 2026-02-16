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

import java.util.List;


public class OrganizationModel {

    private String organizationName;
    private String defaultVdcName;
    private String defaultNetworkName;
    private String tenant;
    private String region;
    private String storage;
    private List<String> allowedShapes;
    private boolean managementAvailable;
    private boolean describeAll;
    private String describer;

    public OrganizationModel() {
        //json
    }

    public String getDescriber() {
        return describer;
    }

    public void setDescriber(String describer) {
        this.describer = describer;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStorage() {
        return storage;
    }

    public boolean isDescribeAll() {
        return describeAll;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getDefaultVdcName() {
        return defaultVdcName;
    }

    public String getDefaultNetworkName() {
        return defaultNetworkName;
    }

    public String getTenant() {
        return tenant;
    }

    public String getRegion() {
        return region;
    }

    public List<String> getAllowedShapes() {
        return allowedShapes;
    }

    public boolean isManagementAvailable() {
        return managementAvailable;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public void setDefaultVdcName(String defaultVdcName) {
        this.defaultVdcName = defaultVdcName;
    }

    public void setDefaultNetworkName(String defaultNetworkName) {
        this.defaultNetworkName = defaultNetworkName;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public void setAllowedShapes(List<String> allowedShapes) {
        this.allowedShapes = allowedShapes;
    }

    public void setManagementAvailable(boolean managementAvailable) {
        this.managementAvailable = managementAvailable;
    }

    public void setDescribeAll(boolean describeAll) {
        this.describeAll = describeAll;
    }
}
