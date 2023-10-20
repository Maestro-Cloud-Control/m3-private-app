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
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.dao.IOpenStackTenantRepository;
import io.maestro3.agent.http.tracker.IHttpRequestTracker;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.model.tenant.OpenStackTenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;


@Service
public class OpenstackHealthCheckService extends AbstractHealthCheckService<OpenStackRegionConfig, OpenStackTenant> {

    @Autowired
    public OpenstackHealthCheckService(MongoTemplate privateAgentMongo,
                                       @Value("${mongo.db.private.agent.uri}") String mongoUri,
                                       IOpenStackRegionRepository cloudRepository,
                                       IOpenStackTenantRepository organizationRepository,
                                       IAmqpMessageTracker amqpMessageTracker,
                                       IHttpRequestTracker httpRequestTracker) {
        super(cloudRepository, organizationRepository, ManagementFactory.getMemoryMXBean(), privateAgentMongo,
            mongoUri, amqpMessageTracker, httpRequestTracker);
    }

    protected void check(String tenantAlias, OpenStackRegionConfig region) {
        // not implemented
    }
}
