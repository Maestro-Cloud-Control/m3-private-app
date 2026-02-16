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

package io.maestro3.agent.admin.factory.impl.vmware;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.OrganizationModel;
import io.maestro3.agent.dao.IVmWareTenantRepository;
import io.maestro3.agent.dao.IVmwareRegionRepository;
import io.maestro3.agent.model.base.TenantState;
import io.maestro3.agent.model.cloud.VCloud;
import io.maestro3.agent.model.vdc.Organization;
import io.maestro3.agent.model.vdc.OrganizationResourceModel;
import io.maestro3.agent.model.vdc.VDC;
import io.maestro3.agent.model.vdc.VDCResourceModel;
import io.maestro3.agent.service.IVirtualizationService;
import io.maestro3.agent.service.configuration.VmwareConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.internal.util.StringUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkOptionItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkSelectItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Service
public class VmwareCreateTenantCommand extends AbstractAdminCommand<OrganizationModel> {

    private final IVmwareRegionRepository repository;
    private final IVmWareTenantRepository tenantRepository;
    private final IVirtualizationService virtualizationService;

    @Autowired
    public VmwareCreateTenantCommand(IVmwareRegionRepository repository, IVmWareTenantRepository tenantRepository,
                                     IVirtualizationService virtualizationService) {
        this.tenantRepository = tenantRepository;
        this.repository = repository;
        this.virtualizationService = virtualizationService;
    }

    @Override
    public OrganizationModel getParams(String body, String... queryParams) {
        OrganizationModel organizationModel = JsonUtils.parseJson(body, OrganizationModel.class);
        organizationModel.setRegion(queryParams[0]);
        return organizationModel;
    }

    @Override
    protected OrganizationModel buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, VmwareConfigurationWizardConstant.TENANT_NAME);
        String regionName = PrivateWizardUtils.getSelectedOptionItem(firstStep, VmwareConfigurationWizardConstant.REGION_NAME_ITEM).getTitle();
        String orgName = PrivateWizardUtils.getSelectedOptionItem(secondStep, VmwareConfigurationWizardConstant.ORGANIZATION_SELECT_ITEM).getTitle();
        String describerMode = PrivateWizardUtils.getSelectedOptionItem(secondStep, VmwareConfigurationWizardConstant.DESCRIBER_SELECT_ITEM).getValue();
        SdkSelectItem managementItem = PrivateWizardUtils.getSelectItem(secondStep, VmwareConfigurationWizardConstant.ENABLE_MANAGEMENT_ITEM);
        SdkOptionItem vdcItem = PrivateWizardUtils.getSelectedOptionItem(thirdStep, VmwareConfigurationWizardConstant.VDC_SELECT_ITEM);
        String network = PrivateWizardUtils.getSelectedOptionItem(vdcItem.getSelect(), VmwareConfigurationWizardConstant.NETWORK_SELECT_ITEM).getTitle();
        String storage = PrivateWizardUtils.getSelectedOptionItem(vdcItem.getSelect(), VmwareConfigurationWizardConstant.STORAGE_PROFILE_SELECT_ITEM).getTitle();
        OrganizationModel model = new OrganizationModel();
        model.setTenant(tenantName);
        model.setRegion(regionName);
        model.setOrganizationName(orgName);
        model.setDescribeAll(VmwareConfigurationWizardConstant.ALL_MODE.equals(describerMode));
        model.setManagementAvailable(managementItem.getOption().get(0).getSelected());
        model.setDefaultVdcName(vdcItem.getTitle());
        model.setDefaultNetworkName(network);
        model.setStorage(storage);
        return model;
    }

    @Override
    public AdminSdkResponse execute(OrganizationModel model) {
        model.setDescribeAll(VmwareConfigurationWizardConstant.ALL_MODE.equals(model.getDescriber()));
        VCloud cloud = repository.findByAliasInCloud(model.getRegion());
        if (Objects.nonNull(tenantRepository.findByOrganizationNameAndRegionId(model.getOrganizationName(), cloud.getId()))) {
            throw new IllegalStateException("ERROR: Organization with name is already registered " + model.getOrganizationName());
        }
        if (Objects.nonNull(tenantRepository.findByTenantAliasAndRegionIdInCloud(model.getTenant(), cloud.getId()))) {
            throw new IllegalStateException("ERROR: Organization with tenant name is already registered " + model.getTenant());
        }
        String organizationHref = virtualizationService.getOrganizationHrefByName(cloud, model.getOrganizationName());
        if (StringUtils.isBlank(organizationHref)) {
            throw new IllegalStateException("ERROR: Organization with specified name is not found");
        }
        Organization organization = buildOrganization(model, cloud, organizationHref);
        virtualizationService.populateWithResources(cloud, organization);
        for (OrganizationResourceModel vdc : organization.getVdcs()) {
            if (Objects.equals(vdc.getName(), model.getDefaultVdcName())) {
                organization.setDefaultVdcHref(vdc.getUrl());
                break;
            }
        }
        if (StringUtils.isBlank(organization.getDefaultVdcHref())) {
            throw new IllegalStateException("ERROR: There is no VDC with name " + model.getDefaultVdcName() + " in organization");
        }
        VDC vdc = virtualizationService.describeVDC(cloud, organization, organization.getDefaultVdcHref());
        String url = null;
        for (VDCResourceModel network : vdc.getNetworks()) {
            if (Objects.equals(network.getName(), model.getDefaultNetworkName())) {
                url = network.getUrl();
                break;
            }
        }
        if (StringUtils.isBlank(url)) {
            throw new IllegalStateException("ERROR: There is no NETWORK with name " + model.getDefaultNetworkName() + " in organization");
        }
        organization.setDefaultNetworkHref(url);

        String storageUrl = null;
        for (VDCResourceModel storage : vdc.getStorageProfiles()) {
            if (Objects.equals(storage.getName(), model.getStorage())) {
                storageUrl = storage.getUrl();
                break;
            }
        }
        if (StringUtils.isBlank(storageUrl)) {
            throw new IllegalStateException("ERROR: There is no STORAGE with name " + model.getStorage() + " in default VDC");
        }
        organization.setDefaultStoragePolicy(storageUrl);
        organization.setTenantState(TenantState.AVAILABLE);
        virtualizationService.populateWithResources(cloud, organization);
        tenantRepository.save(organization);
        return AdminSdkResponse.of("Tenant registered successfully");
    }

    @Override
    public SdkAdminCommand prepareCommand(OrganizationModel params) {
        String template = "m3admin private vmware create_tenant " +
            "--region_alias ${REGION_NAME} " +
            "--organization ${ORGANIZATION} " +
            "--default_vdc ${VDC_NAME} " +
            "--default_network ${NETWORK_NAME} " +
            "--tenant_alias ${TENANT_NAME} " +
            "--describer ${DESCRIBER_MODE} " +
            "--storage_profile ${STORAGE_PROFILE} ";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_NAME", params.getRegion());
        placeholders.put("ORGANIZATION", params.getOrganizationName());
        placeholders.put("VDC_NAME", String.valueOf(params.getDefaultVdcName()));
        placeholders.put("NETWORK_NAME", String.valueOf(params.getDefaultNetworkName()));
        placeholders.put("TENANT_NAME", String.valueOf(params.getTenant()));
        placeholders.put("STORAGE_PROFILE", String.valueOf(params.getStorage()));
        placeholders.put("DESCRIBER_MODE", params.isDescribeAll() ? "ALL" : "OUR");
        if (params.isManagementAvailable()) {
            template += "--management ";
        }
        return new SdkAdminCommand()
            .setType(getType().name())
            .setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.VMWARE_CREATE_TENANT;
    }

    private Organization buildOrganization(OrganizationModel model, VCloud cloud, String organizationHref) {
        Organization organization = new Organization();
        organization.setOrganizationName(model.getOrganizationName());
        organization.setOrganizationHref(organizationHref);
        organization.setRegionId(cloud.getId());
        organization.setTenantAlias(model.getTenant());
        organization.setManagementAvailable(model.isManagementAvailable());
        organization.setDescribeAllInstances(model.isDescribeAll());
        return organization;
    }
}
