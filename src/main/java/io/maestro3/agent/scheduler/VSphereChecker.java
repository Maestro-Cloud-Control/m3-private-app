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

package io.maestro3.agent.scheduler;

import io.maestro3.agent.model.base.PrivateCloudType;
import io.maestro3.agent.vsphere.dao.IVSphereTenantRepository;
import io.maestro3.agent.vsphere.model.VSphereTenant;
import io.maestro3.diagnostic.service.IHealthCheckService;
import io.maestro3.diagnostic.service.impl.VSphereHealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class VSphereChecker extends AbstractScheduler {

    private IHealthCheckService healthCheckService;
    private IVSphereTenantRepository organizationRepository;

    @Autowired
    public VSphereChecker(VSphereHealthCheckService healthCheckService, IVSphereTenantRepository organizationRepository) {
        super(PrivateCloudType.VSPHERE, false);
        this.healthCheckService = healthCheckService;
        this.organizationRepository = organizationRepository;
    }

    @Scheduled(cron = "${cron.healthcheck.organization}")
    public void executeSchedule() {
        super.executeSchedule();
    }

    public void execute() {
        start("Beginning to check vsphere");
        List<VSphereTenant> allOrganizations = organizationRepository.findAllInCloud();
        for (VSphereTenant organization : allOrganizations) {
            try {
                healthCheckService.checkTenantInRegion(organization.getTenantAlias(), organization.getRegionId());
            } catch (Exception ex) {
                LOG.error("Error during health check for tenant {}", organization.getTenantAlias(), ex);
            }
        }
        end("VSphere health check finished");
    }

    @Override
    public String getScheduleTitle() {
        return "VSphere health check";
    }
}
