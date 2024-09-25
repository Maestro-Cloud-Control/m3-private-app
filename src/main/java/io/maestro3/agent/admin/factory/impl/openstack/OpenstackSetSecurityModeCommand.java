package io.maestro3.agent.admin.factory.impl.openstack;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.security.request.SetSecurityModeRequest;
import io.maestro3.agent.admin.model.security.TenantSecurityModeDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.dao.IOpenStackTenantRepository;
import io.maestro3.agent.model.network.SecurityModeConfiguration;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.model.tenant.OpenStackTenant;
import io.maestro3.agent.service.IOpenStackSecurityGroupService;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OpenstackSetSecurityModeCommand extends AbstractAdminCommand<SetSecurityModeRequest> {

    private final IOpenStackRegionRepository regionRepository;
    private final IOpenStackTenantRepository tenantRepository;
    private final IOpenStackSecurityGroupService securityGroupService;

    public OpenstackSetSecurityModeCommand(IOpenStackRegionRepository regionRepository, IOpenStackTenantRepository tenantRepository, IOpenStackSecurityGroupService securityGroupService) {
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
        this.securityGroupService = securityGroupService;
    }

    @Override
    protected SetSecurityModeRequest buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SetSecurityModeRequest getParams(String body, String... queryParams) {
        SetSecurityModeRequest request = JsonUtils.parseJson(body, SetSecurityModeRequest.class);
        request.setRegionAlias(queryParams[0]);
        return request;
    }

    @Override
    public AdminSdkResponse execute(SetSecurityModeRequest request) {
        EntityValidator.validate(request);
        String regionAlias = request.getRegionAlias();
        OpenStackRegionConfig region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        SecurityModeConfiguration newModeConfiguration = region.getSecurityModeConfiguration(request.getNewModeName())
                .orElseThrow(() -> new IllegalStateException("ERROR: "));
        Collection<OpenStackTenant> tenants = getTenants(region, request);
        List<TenantSecurityModeDto> dtos = tenants.stream()
                .map(tenant -> new TenantSecurityModeDto(region.getRegionAlias(), tenant.getTenantAlias(),
                        tenant.getSecurityMode(), newModeConfiguration.getName()))
                .sorted(Comparator.comparing(TenantSecurityModeDto::getRegionAlias).thenComparing(TenantSecurityModeDto::getTenantAlias))
                .collect(Collectors.toList());
        tenants.forEach(tenant -> securityGroupService.updateSecurityType(tenant, region, newModeConfiguration));
        return AdminSdkResponse.of(dtos);
    }

    private Collection<OpenStackTenant> getTenants(OpenStackRegionConfig region, SetSecurityModeRequest request) {
        String tenantAlias = request.getTenantAlias();
        String currentModeName = request.getCurrentModeName();
        if (StringUtils.isNotBlank(tenantAlias)) {
            OpenStackTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantAlias, region.getId());
            if (tenant == null) {
                throw new IllegalStateException("ERROR: Tenant with specified alias is not exist. Received name " + tenantAlias);
            }
            return Collections.singleton(tenant);
        }
        Collection<OpenStackTenant> tenants;
        if (StringUtils.isNotBlank(currentModeName)) {
            tenants = tenantRepository.findProjectsWithSecurityMode(region.getId(), currentModeName);
        } else {
            tenants = tenantRepository.findByRegionIdInCloud(region.getId());
        }
        if (CollectionUtils.isEmpty(tenants)) {
            throw new IllegalStateException("ERROR: No tenants found");
        }
        return tenants;
    }

    @Override
    public SdkAdminCommand prepareCommand(SetSecurityModeRequest request) {
        String template = "m3admin private openstack set_security_mode --region_alias ${REGION_ALIAS} --name ${SECURITY_MODE_NAME} ";
        if (StringUtils.isNotBlank(request.getTenantAlias())) {
            template += "--tenant_alias ${TENANT_ALIAS} ";
        }
        if (StringUtils.isNotBlank(request.getCurrentModeName())) {
            template += "--current_mode_name ${CURRENT_MODE_NAME}";
        }
        Map<String, String> placeholders = Map.of(
                "REGION_ALIAS", request.getRegionAlias(),
                "SECURITY_MODE_NAME", request.getNewModeName(),
                "TENANT_ALIAS", request.getTenantAlias(),
                "CURRENT_MODE_NAME", request.getCurrentModeName()
        );
        return new SdkAdminCommand()
                .setType(getType().name())
                .setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_SET_SECURITY_MODE;
    }
}
