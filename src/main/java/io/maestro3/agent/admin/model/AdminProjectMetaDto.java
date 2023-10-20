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

import io.maestro3.agent.model.AdminProjectMeta;
import io.maestro3.agent.model.PlatformShapeMapping;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class AdminProjectMetaDto extends BaseRegionDto {

    private String projectId;
    private Set<PlatformShapeMappingDto> availablePublicImagesPlatformMapping = new HashSet<>();

    public AdminProjectMetaDto() {
    }

    public AdminProjectMetaDto(AdminProjectMeta adminProjectMeta) {
        this.projectId = adminProjectMeta.getProjectId();
        Set<PlatformShapeMapping> mapping = adminProjectMeta.getAvailablePublicImagesPlatformMapping();
        if (mapping != null) {
            this.availablePublicImagesPlatformMapping = mapping.stream()
                    .map(PlatformShapeMappingDto::new)
                    .collect(Collectors.toSet());
        }
    }

    public AdminProjectMeta toProjectMeta() {
        AdminProjectMeta meta = new AdminProjectMeta();
        meta.setProjectId(this.projectId);
        if (this.availablePublicImagesPlatformMapping != null) {
            meta.setAvailablePublicImagesPlatformMapping(this.availablePublicImagesPlatformMapping.stream()
                    .map(PlatformShapeMappingDto::toShapeMapping)
                    .collect(Collectors.toSet()));
        }
        return meta;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Set<PlatformShapeMappingDto> getAvailablePublicImagesPlatformMapping() {
        return availablePublicImagesPlatformMapping;
    }

    public void setAvailablePublicImagesPlatformMapping(Set<PlatformShapeMappingDto> availablePublicImagesPlatformMapping) {
        this.availablePublicImagesPlatformMapping = availablePublicImagesPlatformMapping;
    }

    @Override
    public String toString() {
        return "AdminProjectMeta{" +
                "projectId='" + projectId + '\'' +
                ", availablePublicImagesPlatformMapping=" + availablePublicImagesPlatformMapping +
                '}';
    }
}
