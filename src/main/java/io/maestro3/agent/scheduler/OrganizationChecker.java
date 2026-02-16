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

import io.maestro3.agent.dao.IVmWareTenantRepository;
import io.maestro3.agent.model.base.PrivateCloudType;
import io.maestro3.agent.model.vdc.Organization;
import io.maestro3.diagnostic.service.IHealthCheckService;
import io.maestro3.diagnostic.service.impl.VCloudHealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class OrganizationChecker extends AbstractScheduler {

    private IHealthCheckService healthCheckService;
    private IVmWareTenantRepository organizationRepository;

    @Autowired
    public OrganizationChecker(VCloudHealthCheckService healthCheckService, IVmWareTenantRepository organizationRepository) {
        super(PrivateCloudType.VMWARE, false);
        this.healthCheckService = healthCheckService;
        this.organizationRepository = organizationRepository;
    }

    @Scheduled(cron = "${cron.healthcheck.organization}")
    public void executeSchedule() {
        super.executeSchedule();
    }

    public void execute() {
        start("Beginning to check organizations");
        List<Organization> allOrganizations = organizationRepository.findAllInCloud();
        for (Organization organization : allOrganizations) {
            try {
                healthCheckService.checkTenantInRegion(organization.getTenantAlias(), organization.getRegionId());
            } catch (Exception ex) {
                LOG.error("Error during healcheck for organization {}", organization.getOrganizationName(), ex);
            }
        }
        end("Organizations healthcheck finished");
    }

    @Override
    public String getScheduleTitle() {
        return "Organization healthcheck";
    }
}
