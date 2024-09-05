package io.maestro3.agent.admin.model.security;

import io.maestro3.agent.model.network.SecurityModeConfiguration;

public class SecurityModeConfigurationDto {

    private final String regionAlias;
    private final String name;
    private final String securityGroupId;
    private final boolean defaultMode;
    private final String description;

    public SecurityModeConfigurationDto(String regionAlias, SecurityModeConfiguration configuration) {
        this.regionAlias = regionAlias;
        this.name = configuration.getName();
        this.securityGroupId = configuration.getAdminSecurityGroupId();
        this.defaultMode = configuration.isDefaultMode();
        this.description = configuration.getDescription();
    }

    public String getRegionAlias() {
        return regionAlias;
    }

    public String getName() {
        return name;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public boolean isDefaultMode() {
        return defaultMode;
    }

    public String getDescription() {
        return description;
    }
}
