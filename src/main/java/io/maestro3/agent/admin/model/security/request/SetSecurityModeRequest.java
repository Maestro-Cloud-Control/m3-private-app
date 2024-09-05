package io.maestro3.agent.admin.model.security.request;

import io.maestro3.agent.admin.model.BaseRegionDto;

public class SetSecurityModeRequest extends BaseRegionDto {

    private String tenantAlias;
    private String currentModeName;
    private String newModeName;

    public String getTenantAlias() {
        return tenantAlias;
    }

    public String getCurrentModeName() {
        return currentModeName;
    }

    public String getNewModeName() {
        return newModeName;
    }
}
