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

import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.model.base.ITenant;
import io.maestro3.agent.model.tenant.OpenStackTenant;

import java.util.Date;


public class TenantDataMapper {
    public static TenantDataContainer map(ITenant tenant, IRegion region) {
        long lastStatusUpdate = tenant.getLastStatusUpdate();
        String updateDate = lastStatusUpdate == 0 ? "N/A" : new Date(lastStatusUpdate).toString();
        TenantDataContainer.TenantDataContainerBuilder builder = TenantDataContainer.builder()
                .withTenantAlias(tenant.getTenantAlias())
                .withRegionAlias(region.getRegionAlias())
                .withState(tenant.getTenantState())
                .withCloud(tenant.getCloud())
                .withShapes(region.getAllowedShapes())
                .withManagementAvailable(tenant.isManagementAvailable())
                .withLastStatusUpdate(updateDate);
        if (tenant instanceof OpenStackTenant) {
            builder.withNativeName(((OpenStackTenant) tenant).getNativeName())
                    .withSecurityGroupName(((OpenStackTenant) tenant).getSecurityGroupName())
                    .withUserdataTemplateId(((OpenStackTenant) tenant).getUserdataTemplateId());
        }
        return builder.build();
    }
}
