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
import io.maestro3.agent.http.tracker.IHttpRequestTracker;
import io.maestro3.agent.model.base.TenantState;
import io.maestro3.agent.vsphere.client.VSphereApiConstants;
import io.maestro3.agent.vsphere.dao.IVSphereRegionRepository;
import io.maestro3.agent.vsphere.dao.IVSphereTenantRepository;
import io.maestro3.agent.vsphere.model.VSphere;
import io.maestro3.agent.vsphere.model.VSphereTenant;
import io.maestro3.agent.vsphere.service.IVSphereVirtualizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;


@Service
public class VSphereHealthCheckService extends AbstractHealthCheckService<VSphere, VSphereTenant> {

    private IVSphereVirtualizationService virtualizationService;

    @Autowired
    public VSphereHealthCheckService(MongoTemplate privateAgentMongo,
                                     @Value("${mongo.db.private.agent.uri}") String mongoUri,
                                     IVSphereRegionRepository cloudRepository,
                                     IVSphereTenantRepository organizationRepository,
                                     IAmqpMessageTracker amqpMessageTracker,
                                     IHttpRequestTracker httpRequestTracker,
                                     IVSphereVirtualizationService virtualizationService) {
        super(cloudRepository, organizationRepository, ManagementFactory.getMemoryMXBean(), privateAgentMongo,
            mongoUri, amqpMessageTracker, httpRequestTracker);
        this.virtualizationService = virtualizationService;
    }

    protected void check(String tenantAlias, VSphere region) {
        VSphereTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant is not exist with tenant alias  " + tenantAlias);
        }
        if (tenant.isSkipHealthCheck()) {
            return;
        }
        tenant.setTenantState(TenantState.CHECKING);
        tenantRepository.save(tenant);
        try {
            String errMsg = null;
            for (String id : region.getNetwork().keySet()) {
                if (errMsg == null && !virtualizationService.isResourceAvailable(region, tenant, VSphereApiConstants.Network.LIST_BY_ID_URL + id)) {
                    errMsg = "ERROR: There is no network with id " + id;
                }
            }

            tenant.setLastStatusUpdate(System.currentTimeMillis());
            if (errMsg != null) {
                tenant.setTenantState(TenantState.NOT_AVAILABLE);
                tenantRepository.save(tenant);
                throw new IllegalStateException(errMsg);
            } else {
                tenant.setTenantState(TenantState.AVAILABLE);
                tenantRepository.save(tenant);
            }
        } catch (Exception ex) {
            tenant.setTenantState(TenantState.NOT_AVAILABLE);
            tenantRepository.save(tenant);
            throw new IllegalStateException("Received unexpected error during health check", ex);
        }
    }
}
