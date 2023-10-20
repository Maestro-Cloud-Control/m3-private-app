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

package io.maestro3.agent.admin.factory.impl.openstack;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.IAdminCommand;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.UserInfoDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class OpenstackSetUserCommand extends AbstractAdminCommand<UserInfoDto> {

    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackSetUserCommand(IOpenStackRegionRepository regionService) {
        this.regionService = regionService;
    }

    @Override
    public UserInfoDto getParams(String body, String... queryParams) {
        UserInfoDto userInfoDto = JsonUtils.parseJson(body, UserInfoDto.class);
        userInfoDto.setRegionAlias(queryParams[0]);
        return userInfoDto;
    }

    @Override
    public UserInfoDto buildRequest(SdkPrivateWizard wizard) {
        return new UserInfoDto();
    }

    @Override
    public AdminSdkResponse execute(UserInfoDto userInfoDto) {
        String regionAlias = userInfoDto.getRegionAlias();
        OpenStackRegionConfig existingRegion = regionService.findByAliasInCloud(regionAlias);
        if (existingRegion == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        EntityValidator.validate(userInfoDto);
        existingRegion.setAdminUserCredentials(userInfoDto.toUserInfo());
        regionService.save(existingRegion);
        return AdminSdkResponse.of("Admin user successfully configured for region " + regionAlias);
    }

    @Override
    public SdkAdminCommand prepareCommand(UserInfoDto params) {
        String template = "m3admin private openstack set_user --region_alias ${REGION_ALIAS} --name ${NAME} " +
            "--password ${PASSWORD} --native_id ${NATIVE_ID}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegionAlias());
        placeholders.put("NAME", params.getDomainName());
        placeholders.put("PASSWORD", params.getPassword());
        placeholders.put("NATIVE_ID", params.getNativeId());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_SET_USER;
    }
}
