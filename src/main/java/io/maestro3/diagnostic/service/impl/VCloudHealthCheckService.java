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

package io.maestro3.diagnostic.service.impl;

import io.maestro3.agent.amqp.tracker.IAmqpMessageTracker;
import io.maestro3.agent.dao.IVmWareTenantRepository;
import io.maestro3.agent.dao.IVmwareRegionRepository;
import io.maestro3.agent.http.tracker.IHttpRequestTracker;
import io.maestro3.agent.model.base.TenantState;
import io.maestro3.agent.model.cloud.VCloud;
import io.maestro3.agent.model.disk.StorageProfile;
import io.maestro3.agent.model.vdc.Organization;
import io.maestro3.agent.service.IVirtualizationService;
import io.maestro3.agent.util.XMLParseUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;


@Primary
@Service
public class VCloudHealthCheckService extends AbstractHealthCheckService<VCloud, Organization> {

    private IVirtualizationService virtualizationService;

    @Autowired
    public VCloudHealthCheckService(MongoTemplate privateAgentMongo,
                                    @Value("${mongo.db.private.agent.uri}") String mongoUri,
                                    IVmwareRegionRepository cloudRepository,
                                    IVmWareTenantRepository organizationRepository,
                                    IAmqpMessageTracker amqpMessageTracker,
                                    IHttpRequestTracker httpRequestTracker,
                                    IVirtualizationService virtualizationService) {
        super(cloudRepository, organizationRepository, ManagementFactory.getMemoryMXBean(), privateAgentMongo,
            mongoUri, amqpMessageTracker, httpRequestTracker);
        this.virtualizationService = virtualizationService;
    }

    protected void check(String tenantAlias, VCloud cloud) {
        Organization organization = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, cloud.getId());
        if (organization == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        if (organization.isSkipHealthCheck()) {
            return;
        }
        organization.setTenantState(TenantState.CHECKING);
        tenantRepository.save(organization);
        try {
            String errMsg = null;
            if (!virtualizationService.checkResource(cloud, organization, XMLParseUtils.ORG_ROOT, organization.getOrganizationHref())) {
                errMsg = "ERROR: There is no active organization in VDC";
            }
            if (errMsg == null && !virtualizationService.isVdcEnabled(cloud, organization, XMLParseUtils.VDC_ROOT, organization.getDefaultVdcHref())) {
                errMsg = "ERROR: Default VDC is not active on VDC";
            }
            if (errMsg == null && !virtualizationService.checkResource(cloud, organization, XMLParseUtils.NETWORK_ROOT, organization.getDefaultNetworkHref())) {
                errMsg = "ERROR: Default Network is not active on VDC";
            }
            if (errMsg == null) {
                StorageProfile storageInfo = virtualizationService.getStorageInfo(cloud, organization.getDefaultStoragePolicy());
                if (storageInfo == null) {
                    errMsg = "ERROR: Default storage profile is not exist or not marked as 'Default'";
                }
            }

            organization.setLastStatusUpdate(System.currentTimeMillis());
            if (errMsg != null) {
                organization.setTenantState(TenantState.NOT_AVAILABLE);
                tenantRepository.save(organization);
                throw new IllegalStateException(errMsg);
            } else {
                organization.setTenantState(TenantState.AVAILABLE);
                tenantRepository.save(organization);
            }
        } catch (Exception ex) {
            organization.setTenantState(TenantState.NOT_AVAILABLE);
            tenantRepository.save(organization);
            throw new IllegalStateException("Received unexpected error during health check", ex);
        }
    }
}
