package io.maestro3.agent.admin.factory.impl.openstack;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.security.request.DeleteSecurityModeRequest;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.dao.IOpenStackTenantRepository;
import io.maestro3.agent.model.network.SecurityModeConfiguration;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OpenstackDeleteSecurityModeCommand extends AbstractAdminCommand<DeleteSecurityModeRequest> {

    private final IOpenStackRegionRepository regionRepository;
    private final IOpenStackTenantRepository tenantRepository;

    @Autowired
    public OpenstackDeleteSecurityModeCommand(IOpenStackRegionRepository regionRepository,
                                              IOpenStackTenantRepository tenantRepository) {
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected DeleteSecurityModeRequest buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteSecurityModeRequest getParams(String body, String... queryParams) {
        DeleteSecurityModeRequest request = JsonUtils.parseJson(body, DeleteSecurityModeRequest.class);
        request.setRegionAlias(queryParams[0]);
        return request;
    }

    @Override
    public AdminSdkResponse execute(DeleteSecurityModeRequest request) {
        EntityValidator.validate(request);
        OpenStackRegionConfig region = regionRepository.findByAliasInCloud(request.getRegionAlias());
        if (region == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + request.getRegionAlias());
        }
        String modeName = request.getName();
        Map<String, SecurityModeConfiguration> securityModeConfigurations = region.getSecurityModeConfigurations();
        if (!securityModeConfigurations.containsKey(modeName)) {
            throw new IllegalStateException(String.format("ERROR: Region %s has no security mode %s configured",
                    region.getRegionAlias(), modeName));
        }

        int projectsCount = tenantRepository.findProjectsWithSecurityMode(region.getId(), modeName).size();
        if (projectsCount > 0) {
            throw new IllegalStateException(String.format("ERROR: There are %d tenants with %s security mode", projectsCount, modeName));
        }
        securityModeConfigurations.remove(modeName);
        regionRepository.save(region);
        return AdminSdkResponse.of(String.format("Security mode %s was successfully deleted for region %s", modeName, region.getRegionAlias()));
    }

    @Override
    public SdkAdminCommand prepareCommand(DeleteSecurityModeRequest request) {
        String template = "m3admin private openstack delete_security_mode " +
                "--region_alias ${REGION_ALIAS} " +
                "--name ${SECURITY_MODE_NAME}";
        Map<String, String> placeholders = Map.of(
                "REGION_ALIAS", request.getRegionAlias(),
                "SECURITY_MODE_NAME", request.getName()
        );
        return new SdkAdminCommand()
                .setType(getType().name())
                .setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_DELETE_SECURITY_MODE;
    }
}
