package io.maestro3.agent.admin.factory.impl.openstack;

import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.security.request.DescribeSecurityModesRequest;
import io.maestro3.agent.admin.model.security.SecurityModeConfigurationDto;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.model.network.SecurityModeConfiguration;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OpenstackDescribeSecurityModesCommand extends AbstractAdminCommand<DescribeSecurityModesRequest> {

    private final IOpenStackRegionRepository regionRepository;

    @Autowired
    public OpenstackDescribeSecurityModesCommand(IOpenStackRegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    protected DescribeSecurityModesRequest buildRequest(SdkPrivateWizard wizard) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DescribeSecurityModesRequest getParams(String body, String... queryParams) {
        return JsonUtils.parseJson(body, DescribeSecurityModesRequest.class);
    }

    @Override
    public AdminSdkResponse execute(DescribeSecurityModesRequest request) {
        Collection<OpenStackRegionConfig> regions = getRegions(request.getRegionAlias());
        List<SecurityModeConfigurationDto> dtos = regions.stream()
                .map(region -> retrieveConfigurationDtos(region, request.getName()))
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(SecurityModeConfigurationDto::getRegionAlias)
                        .thenComparing(SecurityModeConfigurationDto::isDefaultMode)
                        .thenComparing(SecurityModeConfigurationDto::getName))
                .collect(Collectors.toList());
        return AdminSdkResponse.of(dtos);
    }

    private Collection<OpenStackRegionConfig> getRegions(String regionAlias) {
        if (StringUtils.isBlank(regionAlias)) {
            return regionRepository.findAllRegionsForCloud();
        }
        OpenStackRegionConfig region = regionRepository.findByAliasInCloud(regionAlias);
        if (region == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + regionAlias);
        }
        return Collections.singletonList(region);
    }

    private Collection<SecurityModeConfigurationDto> retrieveConfigurationDtos(OpenStackRegionConfig region, String modeName) {
        Map<String, SecurityModeConfiguration> configurations = region.getSecurityModeConfigurations();
        if (MapUtils.isEmpty(configurations)) {
            return Collections.emptyList();
        }
        return configurations.values().stream()
                .filter(configuration -> StringUtils.isBlank(modeName) || Objects.equals(modeName, configuration.getName()))
                .map(configuration -> new SecurityModeConfigurationDto(region.getRegionAlias(), configuration))
                .collect(Collectors.toList());
    }

    @Override
    public SdkAdminCommand prepareCommand(DescribeSecurityModesRequest request) {
        String template = "m3admin private openstack describe_security_modes ";
        if (StringUtils.isNotBlank(request.getRegionAlias())) {
            template += "--region_alias ${REGION_ALIAS} ";
        }
        if (StringUtils.isNotBlank(request.getName())) {
            template += "--name ${SECURITY_MODE_NAME}";
        }

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
        return AdminCommandType.OPEN_STACK_DESCRIBE_SECURITY_MODES;
    }
}
