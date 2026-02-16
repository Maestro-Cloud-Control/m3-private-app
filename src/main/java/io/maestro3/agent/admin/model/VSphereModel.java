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

import io.maestro3.agent.vsphere.model.HardwareVersions;
import io.maestro3.agent.vsphere.model.NetworkPortgroup;
import io.maestro3.agent.vsphere.model.PlacementType;
import io.maestro3.agent.vsphere.model.RegionType;

import java.util.List;
import java.util.Map;


public class VSphereModel {

    private String server;
    private String username;
    private String password;
    private RegionType regionType;
    private String datacenter;
    private String cluster;
    private Map<String, String> network;
    private HardwareVersions hardwareVersion = HardwareVersions.VMX_13;
    private String regionAlias;
    private boolean managementAvailable;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RegionType getRegionType() {
        return regionType;
    }

    public void setRegionType(RegionType regionType) {
        this.regionType = regionType;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public Map<String, String> getNetwork() {
        return network;
    }

    public void setNetwork(Map<String, String> network) {
        this.network = network;
    }

    public HardwareVersions getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(HardwareVersions hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public String getRegionAlias() {
        return regionAlias;
    }

    public void setRegionAlias(String regionAlias) {
        this.regionAlias = regionAlias;
    }

    public boolean isManagementAvailable() {
        return managementAvailable;
    }

    public void setManagementAvailable(boolean managementAvailable) {
        this.managementAvailable = managementAvailable;
    }
}
