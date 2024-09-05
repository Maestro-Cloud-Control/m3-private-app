package io.maestro3.agent.admin.model.security.request;

import io.maestro3.agent.admin.model.BaseRegionDto;

public class ConfigureSecurityModeRequest extends BaseRegionDto {

    private String name;
    private String description;
    private String adminSecurityGroupId;
    private boolean defaultMode;
    private boolean override;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAdminSecurityGroupId() {
        return adminSecurityGroupId;
    }

    public boolean isDefaultMode() {
        return defaultMode;
    }

    public boolean isOverride() {
        return override;
    }
}
