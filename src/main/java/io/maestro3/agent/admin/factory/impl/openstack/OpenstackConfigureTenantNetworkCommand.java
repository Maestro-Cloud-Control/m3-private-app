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

import com.google.common.base.Strings;
import io.maestro3.agent.admin.AdminCommandType;
import io.maestro3.agent.admin.factory.impl.AbstractAdminCommand;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.dao.IOpenStackRegionRepository;
import io.maestro3.agent.dao.IOpenStackTenantRepository;
import io.maestro3.agent.model.base.VLAN;
import io.maestro3.agent.model.network.impl.OpenStackTenantNetworkInfo;
import io.maestro3.agent.model.network.impl.TenantNetworkCreationInputParameters;
import io.maestro3.agent.model.network.impl.vlan.OpenStackVLAN;
import io.maestro3.agent.model.region.OpenStackRegionConfig;
import io.maestro3.agent.model.tenant.OpenStackTenant;
import io.maestro3.agent.openstack.api.networking.bean.FixedIp;
import io.maestro3.agent.openstack.api.networking.bean.Networking;
import io.maestro3.agent.openstack.api.networking.bean.Router;
import io.maestro3.agent.service.IAdminVLANService;
import io.maestro3.agent.service.IOpenStackActivateTenantService;
import io.maestro3.agent.util.ConsoleCommandTokenizer;
import io.maestro3.agent.util.UtilsReadableAssert;
import io.maestro3.sdk.internal.util.CollectionUtils;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;
import io.maestro3.sdk.v3.request.agent.ConfigureTenantNetworkRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenstackConfigureTenantNetworkCommand extends AbstractAdminCommand<ConfigureTenantNetworkRequest> {

    private IOpenStackRegionRepository regionRepository;
    private IOpenStackTenantRepository tenantRepository;
    private IAdminVLANService adminVLANService;
    private IOpenStackActivateTenantService openStackActivateTenantService;

    @Autowired
    public OpenstackConfigureTenantNetworkCommand(IOpenStackRegionRepository regionRepository, IOpenStackTenantRepository tenantRepository,
                                                  IAdminVLANService adminVLANService, IOpenStackActivateTenantService openStackActivateTenantService) {
        this.regionRepository = regionRepository;
        this.tenantRepository = tenantRepository;
        this.adminVLANService = adminVLANService;
        this.openStackActivateTenantService = openStackActivateTenantService;
    }

    @Override
    public ConfigureTenantNetworkRequest getParams(String body, String... queryParams) {
        ConfigureTenantNetworkRequest request = JsonUtils.parseJson(body, ConfigureTenantNetworkRequest.class);
        return ConfigureTenantNetworkRequest.builder()
            .withRegion(queryParams[0])
            .withTenantName(queryParams[1])
            .withName(request.getName())
            .withCidr(request.getCidr())
            .withGatewayNetworkId(request.getGatewayNetworkId())
            .withGatewaySubnetId(request.getGatewaySubnetId())
            .withGatewayIpAddress(request.getGatewayIpAddress())
            .withHighlyAvailable(request.isHighlyAvailable())
            .withDisableSnat(request.isDisableSnat())
            .build();
    }

    @Override
    public ConfigureTenantNetworkRequest buildRequest(SdkPrivateWizard wizard) {
        return ConfigureTenantNetworkRequest.builder().build();
    }

    @Override
    public AdminSdkResponse execute(ConfigureTenantNetworkRequest request) {
        OpenStackRegionConfig region = regionRepository.findByAliasInCloud(request.getRegion());
        if (region == null) {
            throw new IllegalStateException("ERROR: Region with specified alias is not exist. Received name " + request.getRegion());
        }
        String tenantName = request.getTenantName();
        String regionId = region.getId();

        OpenStackTenant tenant = tenantRepository.findByTenantAliasAndRegionIdInCloud(tenantName, regionId);
        if (tenant == null) {
            throw new IllegalStateException("ERROR: Tenant with specified alias is not exist. Received name " + request.getRegion());
        }

        TenantNetworkCreationInputParameters inputParameters = new TenantNetworkCreationInputParameters()
            .setGatewayNetworkId(request.getGatewayNetworkId())
            .setSubnetId(request.getGatewaySubnetId())
            .setIpAddress(request.getGatewayIpAddress())
            .setHighlyAvailable(request.isHighlyAvailable())
            .setDisableSnat(request.isDisableSnat())
            .setTenantName(request.getTenantName())
            .setNetworkName(request.getName())
            .setCidr(request.getCidr());

        checkVLANExist(inputParameters.getNetworkName(), tenant, region);

        Networking networking = openStackActivateTenantService.setupTenantLimitedNetworking(
            inputParameters, tenant, region);

        String vlanName = networking.getNetwork().getName();
        setupTenantVlan(region, tenant, networking, vlanName, inputParameters.getIpAddress());

        OpenStackTenantNetworkInfo.Builder infoBuilder = OpenStackTenantNetworkInfo.build()
            .networkId(networking.getNetwork().getId())
            .networkName(vlanName)
            .subnetName(networking.getSubnet().getName())
            .subnetCidr(networking.getSubnet().getCidr())
            .gatewayNetworkId(networking.getRouter().getExternalGateway().getNetworkId());
        if (inputParameters.getIpAddress() != null) {
            infoBuilder.gatewayExternalIp(inputParameters.getIpAddress());
        }

        return AdminSdkResponse.of(infoBuilder.get());
    }

    private void checkVLANExist(String vlanName, OpenStackTenant openStackProject, OpenStackRegionConfig osZone) {
        String projectId = openStackProject.getId();
        String zoneId = osZone.getId();

        if (!Strings.isNullOrEmpty(vlanName)) {
            VLAN vlan = adminVLANService.getTenantVLANByName(vlanName, projectId, zoneId);
            UtilsReadableAssert.isNull(vlan, String.format("Tenant network with %s name already registered for project %s in zone %s.",
                vlanName, openStackProject.getTenantAlias(), osZone.getRegionAlias()));
        }
    }

    private void setupTenantVlan(OpenStackRegionConfig osZone, OpenStackTenant openStackProject,
                                 Networking networking, String vlanName, String gatewayExternalIp) {
        OpenStackVLAN openStackVLAN = new OpenStackVLAN();
        openStackVLAN.setOpenStackNetworkId(networking.getNetwork().getId());
        openStackVLAN.setRegionId(osZone.getId());
        openStackVLAN.setTenantId(openStackProject.getId());
        openStackVLAN.setDescription("Project SDN " + vlanName);
        openStackVLAN.setName(vlanName);
        openStackVLAN.setDmz(false);
        openStackVLAN.setSdn(true);
        openStackVLAN.setOperationalSearchId(vlanName.toLowerCase());
        openStackVLAN.setSecurityGroupDisabled(false);
        String externalIp = getExternalIp(gatewayExternalIp, networking);
        if (StringUtils.isNotBlank(externalIp)) {
            openStackVLAN.setGatewayExternalIp(externalIp);
        }
        adminVLANService.addTenantVLAN(openStackVLAN, osZone, openStackProject);
    }

    private String getExternalIp(String gatewayExternalIp, Networking networking) {
        if (StringUtils.isNotBlank(gatewayExternalIp)) {
            return gatewayExternalIp;
        }

        Router router = networking.getRouter();
        if (router == null) {
            return null;
        }
        List<FixedIp> fixedIps = router.getExternalGateway().getFixedIps();
        if (CollectionUtils.isEmpty(fixedIps)) {
            return null;
        }

        return fixedIps.get(0).getIpAddress();
    }

    @Override
    public SdkAdminCommand prepareCommand(ConfigureTenantNetworkRequest params) {
        String template = "m3admin private openstack configure_tenant_network "
            + "--region_alias ${REGION_ALIAS} "
            + "--name ${TENANT_USERNAME} "
            + "--network_name ${NETWORK_NAME} "
            + "--cidr ${CIDR} "
            + "--gateway_network_id ${GATEWAY_NETWORK_ID} "
            + "--gateway_subnet_id ${GATEWAY_SUBNET_ID} "
            + "--gateway_ip_address ${GATEWAY_IP_ADDRESS} ";
        if (params.isHighlyAvailable()){
            template += "--highly_available ";
        }
        if (params.isDisableSnat()){
            template += "--disable_snat ";
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("REGION_ALIAS", params.getRegion());
        placeholders.put("TENANT_USERNAME", params.getTenantName());
        placeholders.put("NETWORK_NAME", params.getName());
        placeholders.put("CIDR", params.getCidr());
        placeholders.put("GATEWAY_NETWORK_ID", params.getGatewayNetworkId());
        placeholders.put("GATEWAY_SUBNET_ID", params.getGatewaySubnetId());
        placeholders.put("GATEWAY_IP_ADDRESS", params.getGatewayIpAddress());
        return new SdkAdminCommand().setType(getType().name()).setCommand(ConsoleCommandTokenizer.tokenize(template, placeholders));
    }

    @Override
    public AdminCommandType getType() {
        return AdminCommandType.OPEN_STACK_CONFIGURE_TENANT_NETWORK;
    }
}
