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
import io.maestro3.agent.admin.model.TemplateModel;
import io.maestro3.agent.dao.IVAppTemplateRepository;
import io.maestro3.agent.dao.IVmWareTenantRepository;
import io.maestro3.agent.dao.IVmwareRegionRepository;
import io.maestro3.agent.model.base.ITenant;
import io.maestro3.agent.model.cloud.VCloud;
import io.maestro3.agent.model.template.TemplateState;
import io.maestro3.agent.model.template.VAppTemplate;
import io.maestro3.agent.service.configuration.VmwareConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.internal.util.StringUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTableRowItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class VmwareCreateTemplateCommand extends AbstractAdminCommand<TemplateModel> {

    private final IVmwareRegionRepository repository;
    private final IVAppTemplateRepository vAppTemplateRepository;
    private final IVmWareTenantRepository tenantRepository;

    @Autowired
    public VmwareCreateTemplateCommand(IVmWareTenantRepository tenantRepository,
                                       IVmwareRegionRepository repository,
                                       IVAppTemplateRepository vAppTemplateRepository) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.vAppTemplateRepository = vAppTemplateRepository;
    }

    @Override
    public TemplateModel getParams(String body, String... queryParams) {
        TemplateModel shapeConfigDto = JsonUtils.parseJson(body, TemplateModel.class);
        shapeConfigDto.setRegion(queryParams[0]);
        return shapeConfigDto;
    }

    @Override
    protected TemplateModel buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException("Single command generation is not supported");
    }

    @Override
    public List<TemplateModel> buildRequests(SdkPrivateWizard wizard) {
        List<TemplateModel> models = new ArrayList<>();
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep forthStep = PrivateWizardUtils.getStepById(4, wizard.getStep());
        String tenantName = PrivateWizardUtils.getTextValue(firstStep, VmwareConfigurationWizardConstant.TENANT_NAME);
        String regionName = PrivateWizardUtils.getSelectedOptionItem(firstStep, VmwareConfigurationWizardConstant.REGION_NAME_ITEM).getTitle();
        SdkTableItem templatesTable = PrivateWizardUtils.findItem(forthStep.getData().getTable(),
            VmwareConfigurationWizardConstant.TEMPLATES_TABLE, SdkTableItem::getName);
        List<SdkTableRowItem> selectedRowForTable = PrivateWizardUtils.getSelectedRowForTable(templatesTable);
        for (SdkTableRowItem row : selectedRowForTable) {
            TemplateModel model = new TemplateModel();
            boolean isPublic = row.getHiddenData().get("public") != null;
            model.setName(row.getHiddenData().get("name"));
            model.setVmwareId(row.getHiddenData().get("id"));
            model.setHref(row.getHiddenData().get("href"));
            model.setRegion(regionName);
            model.setTenant(isPublic ? null : tenantName);
            model.setHref(row.getHiddenData().get("href"));
            model.setAlias(row.getHiddenData().get("name"));
            model.setOverwrite(true);
            model.setVmHref("none");
            models.add(model);
        }
        return models;
    }

    @Override
    public AdminSdkResponse execute(TemplateModel templateModel) {
        VCloud vCloud = repository.findByAliasInCloud(templateModel.getRegion());
        if (vCloud == null) {
            throw new IllegalArgumentException("vCloud not found by region alias " + templateModel.getRegion());
        }
        VAppTemplate existingTemplate = vAppTemplateRepository.findByCloudIdAndVmwareId(vCloud.getId(), templateModel.getVmwareId());
        if (existingTemplate != null && !templateModel.isOverwrite()) {
            throw new IllegalArgumentException("vApp template with specified id already exist in cloud " + templateModel.getRegion());
        }
        String orgId = null;
        if (StringUtils.isNotBlank(templateModel.getTenant())) {
            ITenant org = tenantRepository.findByTenantAliasAndRegionId(templateModel.getTenant(), vCloud.getId());
            if (org == null) {
                throw new IllegalArgumentException("Organization is not found by alias " + templateModel.getTenant());
            }
            orgId = org.getId();
        }
        VAppTemplate vAppTemplate = new VAppTemplate();
        vAppTemplate.setHref(templateModel.getHref());
        vAppTemplate.setName(templateModel.getName());
        vAppTemplate.setVmwareId(templateModel.getVmwareId());
        vAppTemplate.setNetworkName(templateModel.getNetworkName());
        vAppTemplate.setVmHref(templateModel.getVmHref());
        vAppTemplate.setCloudId(vCloud.getId());
        vAppTemplate.setOrganizationId(orgId);
        vAppTemplate.setPublic(orgId == null);
        vAppTemplate.setState(TemplateState.AVAILABLE);
        vAppTemplate.setAlias(templateModel.getAlias());
        vAppTemplateRepository.save(vAppTemplate);
        return AdminSdkResponse.of("Template was successfully added");
    }

    @Override
    public SdkAdminCommand prepareCommand(TemplateModel params) {
        String template = "m3admin private vmware create_template " +
            "--region_alias ${REGION_NAME} " +
            "--template_id ${VMWARE_TEMPLATE_ID} " +
            "--template_href ${VMWARE_TEMPLATE_HREF} " +
            "--vm_href ${VM_HREF} " +
            "--template_alias ${TEMPLATE_ALIAS} " +
            "--template ${TEMPLATE_NAME} ";
        if (params.getTenant() != null) {
            template += "--tenant_alias ${TENANT_NAME} ";
        }
        if (params.getNetworkName() != null) {
            template += "--network ${NETWORK_NAME} ";
        }
        if (params.isOverwrite()) {
            template += "--overwrite";
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_NAME", params.getRegion());
        placeholders.put("VMWARE_TEMPLATE_ID", String.valueOf(params.getVmwareId()));
        placeholders.put("VMWARE_TEMPLATE_HREF", String.valueOf(params.getHref()));
        placeholders.put("VM_HREF", String.valueOf(params.getVmHref()));
        placeholders.put("TEMPLATE_NAME", String.valueOf(params.getName()));
        placeholders.put("TEMPLATE_ALIAS", String.valueOf(params.getAlias()));
        placeholders.put("TENANT_NAME", String.valueOf(params.getTenant()));
        placeholders.put("NETWORK_NAME", String.valueOf(params.getNetworkName()));
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.VMWARE_UPLOAD_TEMPLATE;
    }

}
