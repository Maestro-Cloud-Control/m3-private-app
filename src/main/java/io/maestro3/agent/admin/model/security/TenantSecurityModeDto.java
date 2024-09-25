package io.maestro3.agent.admin.model.security;

public class TenantSecurityModeDto {

    private final String regionAlias;
    private final String tenantAlias;
    private final String previousModeName;
    private final String newModeName;

    public TenantSecurityModeDto(String regionAlias, String tenantAlias, String previousModeName, String newModeName) {
        this.regionAlias = regionAlias;
        this.tenantAlias = tenantAlias;
        this.previousModeName = previousModeName;
        this.newModeName = newModeName;
    }

    public String getRegionAlias() {
        return regionAlias;
    }

    public String getTenantAlias() {
        return tenantAlias;
    }

    public String getPreviousModeName() {
        return previousModeName;
    }

    public String getNewModeName() {
        return newModeName;
    }
}
