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
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.EntityValidator;
import io.maestro3.agent.admin.model.RegionConfigDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.agent.model.enums.OpenStackVersion;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.openstack.provider.OpenStackApiRequest;
import io.maestro3.agent.service.DbServicesProvider;
import io.maestro3.agent.service.proccessor.OpenstackConfigurationWizardConstant;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.PrivateWizardUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.internal.util.StringUtils;
import io.maestro3.sdk.v3.model.SdkCloud;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateStep;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkOptionItem;
import io.maestro3.sdk.v3.model.agent.wizard.item.SdkTextItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;


@Component
public class OpenstackCreateRegionCommand extends AbstractAdminCommand<RegionConfigDto> {

    private final DbServicesProvider dbServicesProvider;
    private final IOpenStackRegionRepository regionService;

    @Autowired
    public OpenstackCreateRegionCommand(DbServicesProvider dbServicesProvider,
                                        IOpenStackRegionRepository regionService) {
        this.dbServicesProvider = dbServicesProvider;
        this.regionService = regionService;
    }

    @Override
    public RegionConfigDto getParams(String body, String... queryParams) {
        return JsonUtils.parseJson(body, RegionConfigDto.class);
    }

    @Override
    public RegionConfigDto buildRequest(SdkPrivateWizard wizard) {

        OpenStackApiRequest.OpenStackApiRequestBuilder requestBuilder = new OpenStackApiRequest.OpenStackApiRequestBuilder();
        SdkPrivateStep firstStep = PrivateWizardUtils.getStepById(1, wizard.getStep());
        SdkPrivateStep secondStep = PrivateWizardUtils.getStepById(2, wizard.getStep());
        for (Map.Entry<String, BiFunction<OpenStackApiRequest.OpenStackApiRequestBuilder, String, OpenStackApiRequest.OpenStackApiRequestBuilder>> entry :
            OpenstackConfigurationWizardConstant.REGION_REQUEST_FILLERS.entrySet()) {
            SdkTextItem item = PrivateWizardUtils.getTextItem(firstStep, entry.getKey());
            String parameterValue = item.getValue();
            if (!StringUtils.isBlank(parameterValue)) {
                entry.getValue().apply(requestBuilder, parameterValue.trim());
            } else {
                throw new IllegalArgumentException("Wizard is invalid, command can't be executed");
            }
        }
        String regionName = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.REGION_NAME_ITEM);
        String serverNamePrefix = PrivateWizardUtils.getTextValue(secondStep, OpenstackConfigurationWizardConstant.SERVER_NAME_PREFIX_ITEM);
        SdkOptionItem osVersionSelect = PrivateWizardUtils.getSelectedOptionItem(firstStep.getData().getSelect(), OpenstackConfigurationWizardConstant.OS_VERSION_ITEM);
        requestBuilder.setVersion(OpenStackVersion.valueOf(osVersionSelect.getValue()));
        OpenStackApiRequest openStackApiRequest = requestBuilder.build();
        List<IRegion> regions = regionService.findByCloud(SdkCloud.OPEN_STACK.name());
        RegionConfigDto regionConfigDto = new RegionConfigDto();
        regionConfigDto.setNameAlias(regionName);
        regionConfigDto.setRegionNumber(regions.size());
        regionConfigDto.setOsVersion(openStackApiRequest.getVersion());
        regionConfigDto.setServerNamePrefix(serverNamePrefix);
        regionConfigDto.setEnableScheduledDescribers(Boolean.TRUE);
        regionConfigDto.setKeystoneAuthUrl(openStackApiRequest.getAuthUrl());
        regionConfigDto.setNativeRegionName(openStackApiRequest.getRegionName());
        return regionConfigDto;
    }

    @Override
    public AdminSdkResponse execute(RegionConfigDto regionConfigDto) {
        IRegion existingRegion = regionService.findByAliasInCloud(regionConfigDto.getNameAlias());
        if (existingRegion != null) {
            throw new IllegalStateException("ERROR: Region with specified alias is already registered. Received name " + regionConfigDto.getNameAlias());
        }
        EntityValidator.validate(regionConfigDto);
        OpenStackRegionConfig openStackRegionConfig = regionConfigDto.toRegionConfig();
        regionService.save(openStackRegionConfig);
        existingRegion = regionService.findByAliasInCloud(regionConfigDto.getNameAlias());
        dbServicesProvider.getPersistenceCountersService().addInstanceInZoneCounter(existingRegion.getId());
        return AdminSdkResponse.of("Region was successfully created");
    }

    @Override
    public SdkAdminCommand prepareCommand(RegionConfigDto params) {
        String template = "m3admin private openstack create_region --name_alias ${NAME_ALIAS} --region_number ${REGION_NUMBER} " +
            "--server_prefix ${SERVER_NAME_PREFIX} --keystone_auth_url ${KEYSTONE_AUTH_URL} " +
            "--region ${REGION_NAME} --openstack_version ${OPENSTACK_VERSION}";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("NAME_ALIAS", params.getNameAlias());
        placeholders.put("REGION_NUMBER", Integer.toString(params.getRegionNumber()));
        placeholders.put("SERVER_NAME_PREFIX", params.getServerNamePrefix());
        placeholders.put("KEYSTONE_AUTH_URL", params.getKeystoneAuthUrl());
        placeholders.put("REGION_NAME", params.getNativeRegionName());
        placeholders.put("OPENSTACK_VERSION", params.getOsVersion().name());
        if (params.isEnableScheduledDescribers()) {
            template += " --enable_describers";
        }
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CREATE_REGION;
    }
}
