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

package io.maestro3.agent.admin.factory.impl.vsphere;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.VSphereTenantModel;
import io.maestro3.agent.exception.ReadableAgentException;
import io.maestro3.agent.model.base.TenantState;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.agent.vsphere.dao.IVSphereRegionRepository;
import io.maestro3.agent.vsphere.dao.IVSphereTenantRepository;
import io.maestro3.agent.vsphere.model.DeployType;
import io.maestro3.agent.vsphere.model.PlacementType;
import io.maestro3.agent.vsphere.model.RegionType;
import io.maestro3.agent.vsphere.model.VSphere;
import io.maestro3.agent.vsphere.model.VSphereTenant;
import io.maestro3.agent.vsphere.service.configuration.VSphereConfigurationWizardConstant;
import io.maestro3.sdk.internal.util.Assert;
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
public class VSphereCreateTenantCommand extends AbstractAdminCommand<VSphereTenantModel> {

    private final IVSphereRegionRepository repository;
    private final IVSphereTenantRepository tenantRepository;

    @Autowired
    public VSphereCreateTenantCommand(IVSphereRegionRepository repository, IVSphereTenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
        this.repository = repository;
    }

    @Override
    public VSphereTenantModel getParams(String body, String... queryParams) {
        VSphereTenantModel organizationModel = JsonUtils.parseJson(body, VSphereTenantModel.class);
        organizationModel.setRegion(queryParams[0]);
        return organizationModel;
    }

    @Override
    protected VSphereTenantModel buildRequest(SdkPrivateWizard wizard) {
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        SdkPrivateStep thirdStep = PrivateWizardUtils.getStepById(3, wizard.getStep());
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, VSphereConfigurationWizardConstant.TENANT_NAME);
        String regionName = PrivateWizardUtils.getSelectedOptionItem(firstStep, VSphereConfigurationWizardConstant.REGION_NAME_ITEM).getTitle();
        String describerMode = PrivateWizardUtils.getSelectedOptionItem(secondStep, VSphereConfigurationWizardConstant.DESCRIBER_SELECT_ITEM).getValue();
        SdkSelectItem managementItem = PrivateWizardUtils.getSelectItem(secondStep, VSphereConfigurationWizardConstant.ENABLE_MANAGEMENT_ITEM);
        SdkOptionItem placementItem = PrivateWizardUtils.getSelectedOptionItem(secondStep, VSphereConfigurationWizardConstant.PLACEMENT_SELECT_ITEM);
        String placementType = placementItem.getValue();
        VSphere sphere = repository.findByAliasInCloud(regionName);
        SdkOptionItem deployItem = PrivateWizardUtils.getSelectedOptionItem(placementItem.getSelect(), VSphereConfigurationWizardConstant.DEPLOY_SELECT_ITEM);
        String deployType = deployItem.getValue();
        RegionType regionType = sphere.getRegionType();

        VSphereTenantModel model = new VSphereTenantModel();
        model.setTenant(tenantName);
        model.setRegion(regionName);
        model.setDeployType(DeployType.valueOf(deployType));
        model.setPlacementType(PlacementType.valueOf(placementType));
        model.setDescribeAll(VSphereConfigurationWizardConstant.ALL_MODE.equals(describerMode));
        model.setManagementAvailable(managementItem.getOption().get(0).getSelected());
        model.setDescriber(describerMode);

        if (model.getDeployType() == DeployType.CONTENT_TEMPLATE) {
            SdkOptionItem libraryItem = PrivateWizardUtils.getSelectedOptionItem(deployItem.getSelect(), VSphereConfigurationWizardConstant.LIBRARY_SELECT_ITEM);
            String libraryId = libraryItem.getValue();
            model.setLibraryId(libraryId);
        } else {
            SdkOptionItem datastoreItem = PrivateWizardUtils.getSelectedOptionItem(thirdStep, VSphereConfigurationWizardConstant.DATASTORE_SELECT_ITEM);
            model.setDatastoreId(datastoreItem.getValue());
            model.setDatastoreName(datastoreItem.getTitle());
            String isoPath = PrivateWizardUtils.getTextValue(thirdStep, VSphereConfigurationWizardConstant.IMAGE_PATH_ITEM);
            model.setIsoFolder(isoPath);
        }

        switch (regionType) {
            case VCENTER:
                SdkOptionItem datacenterItem = PrivateWizardUtils.getSelectedOptionItem(secondStep, VSphereConfigurationWizardConstant.DATACENTER_SELECT_ITEM);
                model.setDatacenterId(datacenterItem.getValue());
                break;
            case DATACENTER:
                SdkOptionItem clusterItem = PrivateWizardUtils.getSelectedOptionItem(secondStep, VSphereConfigurationWizardConstant.CLUSTER_SELECT_ITEM);
                model.setDatacenterId(sphere.getDatacenter());
                model.setClusterId(clusterItem.getValue());
                break;
            case CLUSTER:
                SdkOptionItem folderItem = PrivateWizardUtils.getSelectedOptionItem(secondStep, VSphereConfigurationWizardConstant.FOLDERS_SELECT_ITEM);
                model.setDatacenterId(sphere.getDatacenter());
                model.setClusterId(sphere.getCluster());
                model.setVmFolder(folderItem.getDescription());
                break;
            default:
                throw new ReadableAgentException("Unknown region type");
        }
        return model;
    }

    @Override
    public AdminSdkResponse execute(VSphereTenantModel model) {
        VSphere sphere = repository.findByAliasInCloud(model.getRegion());
        model.setDescribeAll(VSphereConfigurationWizardConstant.ALL_MODE.equals(model.getDescriber()));
        if (Objects.nonNull(tenantRepository.findByTenantAliasAndRegionIdInCloud(model.getTenant(), sphere.getId()))) {
            throw new IllegalStateException("ERROR: Organization with tenant name is already registered " + model.getTenant());
        }
        VSphereTenant tenant = new VSphereTenant();
        tenant.setDescribeAllInstances(model.isDescribeAll());
        tenant.setManagementAvailable(model.isManagementAvailable());
        tenant.setTenantAlias(model.getTenant());
        tenant.setRegionId(sphere.getId());
        tenant.setTenantState(TenantState.AVAILABLE);
        tenant.setClusterId(model.getClusterId());
        tenant.setVmFolder(model.getVmFolder());
        tenant.setHost(model.getHost());
        tenant.setDatacenterId(model.getDatacenterId());
        tenant.setDatastoreName(model.getDatastoreName());
        tenant.setDatastoreId(model.getDatastoreId());
        tenant.setIsoFolder(model.getIsoFolder());
        tenant.setLibraryId(model.getLibraryId());
        tenant.setPlacementType(model.getPlacementType());
        tenant.setDeployType(model.getDeployType());
        if (tenant.getDeployType() == DeployType.CONTENT_TEMPLATE) {
            Assert.hasText(tenant.getLibraryId(), "library_id");
        } else {
            Assert.hasText(tenant.getIsoFolder(), "iso_folder");
            Assert.hasText(tenant.getDatastoreId(), "datastore_id");
            Assert.hasText(tenant.getDatastoreName(), "datastore_name");
        }
        switch (sphere.getRegionType()) {
            case VCENTER:
                Assert.hasText(tenant.getDatacenterId(), "datacenter_id");
                break;
            case DATACENTER:
                tenant.setDatacenterId(sphere.getDatacenter());
                Assert.hasText(tenant.getClusterId(), "cluster_id");
                break;
            case CLUSTER:
                tenant.setDatacenterId(sphere.getDatacenter());
                tenant.setClusterId(sphere.getCluster());
                Assert.hasText(tenant.getVmFolder(), "vm_folder");
                break;
            default:
                throw new ReadableAgentException("Unknown region type");
        }
        tenantRepository.save(tenant);
        return AdminSdkResponse.of("Tenant registered successfully");
    }

    @Override
    public SdkAdminCommand prepareCommand(VSphereTenantModel params) {
        Map<String, String> placeholders = new HashMap<>();
        String template = "m3admin private vsphere create_tenant " +
            addNonEmpty("region_alias", "REGION_NAME", params.getRegion(), placeholders) +
            addNonEmpty("tenant_alias", "TENANT_NAME", params.getTenant(), placeholders) +
            addNonEmpty("cluster_id", "CLUSTER", params.getClusterId(), placeholders) +
            addNonEmpty("vm_folder", "VM_FOLDER", params.getVmFolder(), placeholders) +
            addNonEmpty("host", "HOST", params.getHost(), placeholders) +
            addNonEmpty("datacenter_id", "DATACENTER", params.getDatacenterId(), placeholders) +
            addNonEmpty("datastore_id", "DATASTORE_ID", params.getDatastoreId(), placeholders) +
            addNonEmpty("datastore", "DATASTORE_NAME", params.getDatastoreName(), placeholders) +
            addNonEmpty("iso_folder", "ISO_PATH", params.getIsoFolder(), placeholders) +
            addNonEmpty("library_id", "LIBRARY_ID", params.getLibraryId(), placeholders) +
            addNonEmpty("placement_type", "PLACEMENT_TYPE", params.getPlacementType().name(), placeholders) +
            addNonEmpty("deploy_type", "DEPLOY_TYPE", params.getDeployType().name(), placeholders) +
            "--describer ${DESCRIBER_MODE} ";
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
        return AdminCommandType.VSPHERE_CREATE_TENANT;
    }

    private String addNonEmpty(String param, String placeholder, String value, Map<String, String> placeholders) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        placeholders.put(placeholder, value);
        return String.format("--%s ${%s} ", param, placeholder);
    }
}
