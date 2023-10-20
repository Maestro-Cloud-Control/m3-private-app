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

package io.maestro3.agent.admin.factory.impl.openstack;

import com.fasterxml.jackson.core.type.TypeReference;
import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.DescribeAllRequest;
import io.maestro3.agent.dao.IRegionRepository;
import io.maestro3.agent.dao.ITenantRepository;
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.model.base.ITenant;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkOptionItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class OpenstackSetTenantDescriberModeCommand extends AbstractAdminCommand<DescribeAllRequest> {

    private final IRegionRepository regionRepository;
    private final ITenantRepository tenantRepository;

    @Autowired
    public OpenstackSetTenantDescriberModeCommand(IRegionRepository regionRepository,
                                                  ITenantRepository tenantRepository) {
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public DescribeAllRequest getParams(String body, String... queryParams) {
        DescribeAllRequest request = JsonUtils.parseJson(body, new TypeReference<DescribeAllRequest>() {});
        request.setRegionAlias(queryParams[0]);
        request.setTenantAlias(queryParams[1]);
        return request;
    }

    @Override
    public DescribeAllRequest buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkOptionItem regionOption = PrivateWizardUtils.getSelectedOptionItem(firstStep.getData().getSelect(), OpenstackConfigurationWizardConstant.REGION_SELECT_ITEM);
        String regionName = regionOption.getValue();
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, OpenstackConfigurationWizardConstant.TENANT_NAME);
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        SdkOptionItem describerItem = PrivateWizardUtils.getSelectedOptionItem(thirdStep.getData().getSelect(), OpenstackConfigurationWizardConstant.DESCRIBER_SELECT_ITEM);
        boolean allMode = describerItem.getValue().equals(OpenstackConfigurationWizardConstant.ALL_MODE);

        DescribeAllRequest describeAllRequest = new DescribeAllRequest();
        describeAllRequest.setDescribeAll(allMode);
        describeAllRequest.setTenantAlias(tenantName);
        describeAllRequest.setRegionAlias(regionName);
        return describeAllRequest;
    }

    @Override
    public AdminSdkResponse execute(DescribeAllRequest request) {
        String tenantAlias = request.getTenantAlias();
        String regionAlias = request.getRegionAlias();
        IRegion existingRegion = regionRepository.findByRegionAlias(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        String regionId = existingRegion.getId();
        ITenant existingTenant = tenantRepository
            .findByTenantAliasAndRegionId(tenantAlias, regionId);
        if (existingTenant == null) {
            throw new IllegalStateException("ERROR: Tenant with specified alias not found. Received name " + tenantAlias);
        }
        if (existingTenant.isDescribeAllInstances() == request.isDescribeAll()) {
            return AdminSdkResponse.of(existingTenant.isDescribeAllInstances()
                ? "Describe for all VMs is already enabled"
                : "Describe for all VMs is already disabled");
        }
        existingTenant.setDescribeAllInstances(request.isDescribeAll());
        tenantRepository.save(existingTenant);
        return AdminSdkResponse.of(existingTenant.isDescribeAllInstances()
            ? "Describe for all VMs was enabled"
            : "Describe for all VMs was disabled");
    }

    @Override
    public SdkAdminCommand prepareCommand(DescribeAllRequest params) {
        String template = "m3admin private system set_tenant_describer_mode --region_alias ${REGION_ALIAS} " +
            "--tenant_alias ${TENANT_ALIAS} --mode ${MODE}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("TENANT_ALIAS", params.getTenantAlias());
        placeholders.put("MODE", params.isDescribeAll() ? "ALL" : "OUR");
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_SET_TENANT_DESCRIBER_MODE;
    }
}
