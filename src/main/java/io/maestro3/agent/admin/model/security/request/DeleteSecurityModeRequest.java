package io.maestro3.agent.admin.model.security.request;

import io.maestro3.agent.admin.model.BaseRegionDto;

public class DeleteSecurityModeRequest extends BaseRegionDto {

    private String name;

    public String getName() {
        return name;
    }
}
