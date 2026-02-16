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


public class NutanixRegionModel {

    private String server;
    private String username;
    private String password;
    private String elementServer;
    private String elementUsername;
    private String elementPassword;
    private String regionAlias;
    private String clusterName;
    private boolean managementAvailable;
    private String clusterUuid;

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

    public String getElementServer() {
        return elementServer;
    }

    public void setElementServer(String elementServer) {
        this.elementServer = elementServer;
    }

    public String getElementUsername() {
        return elementUsername;
    }

    public void setElementUsername(String elementUsername) {
        this.elementUsername = elementUsername;
    }

    public String getElementPassword() {
        return elementPassword;
    }

    public void setElementPassword(String elementPassword) {
        this.elementPassword = elementPassword;
    }

    public String getRegionAlias() {
        return regionAlias;
    }

    public void setRegionAlias(String regionAlias) {
        this.regionAlias = regionAlias;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public boolean isManagementAvailable() {
        return managementAvailable;
    }

    public void setManagementAvailable(boolean managementAvailable) {
        this.managementAvailable = managementAvailable;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}
