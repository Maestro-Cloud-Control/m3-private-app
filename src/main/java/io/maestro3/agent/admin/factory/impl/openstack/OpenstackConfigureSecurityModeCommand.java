package io.maestro3.agent.admin.factory.impl.openstack;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.security.request.ConfigureSecurityModeRequest;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.network.SecurityModeConfiguration;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.service.IOpenStackSecurityGroupService;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class OpenstackConfigureSecurityModeCommand extends AbstractAdminCommand<ConfigureSecurityModeRequest> {

    private final IOpenStackRegionRepository regionRepository;
    private final IOpenStackSecurityGroupService securityGroupService;

    @Autowired
    public OpenstackConfigureSecurityModeCommand(IOpenStackRegionRepository regionRepository,
                                                 IOpenStackSecurityGroupService securityGroupService) {
        this.regionRepository = regionRepository;
        this.securityGroupService = securityGroupService;
    }


    @Override
    protected ConfigureSecurityModeRequest buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConfigureSecurityModeRequest getParams(String body, String... queryParams) {
        ConfigureSecurityModeRequest request = JsonUtils.parseJson(body, ConfigureSecurityModeRequest.class);
        request.setRegionAlias(queryParams[0]);
        return request;
    }

    @Override
    public AdminSdkResponse execute(ConfigureSecurityModeRequest request) {
        EntityValidator.validate(request);
        OpenStackRegionConfig region = regionRepository.findByAliasInCloud(request.getRegionAlias());
        if (region == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + request.getRegionAlias());
        }
        if (request.isOverride()) {
            boolean updated = overrideConfiguration(region, request);
            if (!updated) {
                return AdminSdkResponse.of("Nothing to update");
            }
        } else {
            createConfiguration(region, request);
        }
        return AdminSdkResponse.of(String.format("Security mode %s was successfully configured for region %s", request.getName(), region.getRegionAlias()));
    }

    private boolean overrideConfiguration(OpenStackRegionConfig region,
                                       ConfigureSecurityModeRequest request) {
        Map<String, SecurityModeConfiguration> securityModeConfigurations = region.getSecurityModeConfigurations();
        SecurityModeConfiguration configuration = securityModeConfigurations.get(request.getName());
        if (configuration == null) {
            throw new IllegalStateException(String.format("ERROR: Region with specified alias %s has no security mode '%s' configured.",
                    region.getRegionAlias(), request.getName()));
        }
        boolean updateRequired = updateDescription(configuration, request.getDescription());
        updateRequired |= updateDefaultMode(securityModeConfigurations, request);
        if (updateRequired) {
            regionRepository.save(region);
        }
        return updateRequired;
    }

    private boolean updateDescription(SecurityModeConfiguration configuration, String requestDescription) {
        if (StringUtils.isBlank(requestDescription) || Objects.equals(configuration.getDescription(), requestDescription)) {
            return false;
        }
        configuration.setDescription(requestDescription);
        return true;
    }

    private boolean updateDefaultMode(Map<String, SecurityModeConfiguration> securityModeConfigurations,
                                      ConfigureSecurityModeRequest request) {
        SecurityModeConfiguration configuration = securityModeConfigurations.get(request.getName());
        if (!request.isDefaultMode()) {
            if (configuration.isDefaultMode()) {
                configuration.setDefaultMode(false);
                return true;
            }
            return false;
        }
        if (configuration.isDefaultMode()) {
            return false;
        }
        securityModeConfigurations.values().forEach(config -> config.setDefaultMode(false));
        configuration.setDefaultMode(true);
        return true;
    }

    private void createConfiguration(OpenStackRegionConfig region,
                                     ConfigureSecurityModeRequest request) {
        Map<String, SecurityModeConfiguration> securityModeConfigurations = region.getSecurityModeConfigurations();
        String modeName = request.getName();
        String adminSecurityGroupId = request.getAdminSecurityGroupId();
        if (securityModeConfigurations.containsKey(modeName)) {
            throw new IllegalStateException(String.format("ERROR: Region with specified alias %s already has security mode '%s' configured.",
                    region.getRegionAlias(), modeName));
        }
        if (!securityGroupService.isSecurityGroupExist(region, adminSecurityGroupId)) {
            throw new IllegalStateException(String.format("ERROR: Admin security group with ID %s not found for region %s",
                    adminSecurityGroupId, region.getRegionAlias()));
        }
        if (request.isDefaultMode()) {
            Optional<SecurityModeConfiguration> defaultMode = region.getDefaultSecurityModeConfiguration();
            if (defaultMode.isPresent()) {
                throw new IllegalStateException(String.format("ERROR: Region %s already has default security mode: %s",
                        region.getRegionAlias(), defaultMode.get().getName()));
            }
        }
        SecurityModeConfiguration modeConfiguration = new SecurityModeConfiguration(modeName, request.getDescription(),
                adminSecurityGroupId, request.isDefaultMode());
        securityModeConfigurations.put(modeName, modeConfiguration);
        regionRepository.save(region);
    }

    @Override
    public SdkAdminCommand prepareCommand(ConfigureSecurityModeRequest request) {
        String template = "m3admin private openstack configure_security_mode " +
                "--region_alias ${REGION_ALIAS} " +
                "--name ${SECURITY_MODE_NAME} " +
                "--description ${SECURITY_MODE_DESCRIPTION} " +
                "--security_group_id ${SECURITY_GROUP_ID} ";
        if (request.isDefaultMode()) {
            template += "--default ";
        }
        if (request.isOverride()) {
            template += "--override ";
        }

        Map<String, String> placeholders = Map.of(
                "REGION_ALIAS", request.getRegionAlias(),
                "SECURITY_MODE_NAME", request.getName(),
                "SECURITY_MODE_DESCRIPTION", request.getDescription(),
                "SECURITY_GROUP_ID", request.getAdminSecurityGroupId()
        );
        return new SdkAdminCommand()
                .setType(getType().name())
                .setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CONFIGURE_SECURITY_MODE;
    }
}
