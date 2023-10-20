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

package io.maestro3.agent.admin.model;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;


public class EntityValidator {

    public static void validate(RegionConfigDto regionConfigDto) {
        if (StringUtils.isBlank(regionConfigDto.getNameAlias())) {
            throw new IllegalStateException("ERROR: Name alias should be specified ");
        }
        if (StringUtils.isBlank(regionConfigDto.getServerNamePrefix())) {
            throw new IllegalStateException("ERROR: Server name prefix should be specified ");
        }
        if (StringUtils.isBlank(regionConfigDto.getKeystoneAuthUrl())) {
            throw new IllegalStateException("ERROR: Keystore auth url should be specified ");
        }
        if (regionConfigDto.getRegionNumber() < 0) {
            throw new IllegalStateException("ERROR: Region number should be 0 or more");
        }
    }
    public static void validate(ShapeConfigDto configDto) {
        if (StringUtils.isBlank(configDto.getNativeId())) {
            throw new IllegalStateException("ERROR: Native id should be specified ");
        }
        if (StringUtils.isBlank(configDto.getNativeName())) {
            throw new IllegalStateException("ERROR: Native name should be specified ");
        }
        if (StringUtils.isBlank(configDto.getNameAlias())) {
            throw new IllegalStateException("ERROR: Name alias should be specified ");
        }
        if (configDto.getCpuCount() <= 0) {
            throw new IllegalStateException("ERROR: Cpu count should be more then 0 ");
        }
        if (configDto.getDiskSizeMb() <= 0) {
            throw new IllegalStateException("ERROR: Disk size should be more then 0 ");
        }
        if (configDto.getMemorySizeMb() <= 0) {
            throw new IllegalStateException("ERROR: Name alias should be more then 0 ");
        }
    }

    public static void validate(TenantDto tenantDto) {
        if (StringUtils.isBlank(tenantDto.getNativeId())) {
            throw new IllegalStateException("ERROR: Native id should be specified ");
        }
        if (StringUtils.isBlank(tenantDto.getNetworkId())) {
            throw new IllegalStateException("ERROR: Network id should be specified ");
        }
        if (StringUtils.isBlank(tenantDto.getNativeName())) {
            throw new IllegalStateException("ERROR: Native name should be specified ");
        }
        if (StringUtils.isBlank(tenantDto.getNameAlias())) {
            throw new IllegalStateException("ERROR: Name alias should be specified ");
        }
    }

    public static void validate(ImageDto image) {
        if (StringUtils.isBlank(image.getNameAlias())) {
            throw new IllegalStateException("ERROR: Name alias should be specified ");
        }
        if (StringUtils.isBlank(image.getImageStatus())) {
            throw new IllegalStateException("ERROR: Image status should be specified ");
        }
        if (StringUtils.isBlank(image.getNativeId())) {
            throw new IllegalStateException("ERROR: Native id should be specified ");
        }
        if (StringUtils.isBlank(image.getNativeName())) {
            throw new IllegalStateException("ERROR: Native name should be specified ");
        }
        if (Objects.isNull(image.getPlatformType())) {
            throw new IllegalStateException("ERROR: Platform type should be specified ");
        }
        if (Objects.isNull(image.getImageVisibility())) {
            throw new IllegalStateException("ERROR: Image visibility should be specified ");
        }
    }

    public static void validate(ServiceTenantDto serviceTenantDto) {
        if (StringUtils.isBlank(serviceTenantDto.getName())) {
            throw new IllegalStateException("ERROR: Name should be specified ");
        }
    }

    public static void validate(UserInfoDto userInfoDto) {
        if (StringUtils.isBlank(userInfoDto.getName())) {
            throw new IllegalStateException("ERROR: Name should be specified ");
        }
        if (StringUtils.isBlank(userInfoDto.getNativeId())) {
            throw new IllegalStateException("ERROR: Native id name should be specified ");
        }
        if (StringUtils.isBlank(userInfoDto.getPassword())) {
            throw new IllegalStateException("ERROR: Password should be specified ");
        }
    }

    public static void validate(AdminProjectMetaDto projectMetaDto) {
        if (StringUtils.isBlank(projectMetaDto.getProjectId())) {
            throw new IllegalStateException("ERROR: Project id should be specified ");
        }
    }

    public static void validate(PlatformShapeMappingDto dto) {
        if (StringUtils.isBlank(dto.getImageName())) {
            throw new IllegalStateException("ERROR: Shape alias should be specified ");
        }
        if (StringUtils.isBlank(dto.getPlatformType())) {
            throw new IllegalStateException("ERROR: Platform type should be specified ");
        }
    }

    public static void validate(RabbitConfigDto config) throws IllegalStateException {
        if (config.getMaxConcurrentConsumers() > 30 || config.getMaxConcurrentConsumers() < 1) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. maxConcurrentConsumers should be between 1 and 30");
        }
        if (config.getMinConcurrentConsumers() > 30 || config.getMinConcurrentConsumers() < 1) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. minConcurrentConsumers should be between 1 and 30");
        }
        if (config.getReplyTimeoutMillis() > 3000000 || config.getReplyTimeoutMillis() < 10000) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. replyTimeoutMillis should be between 10000 and 3000000");
        }
        if (config.getShutdownTimeoutMillis() > 3000000 || config.getShutdownTimeoutMillis() < 10000) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. replyTimeoutMillis should be between 10000 and 3000000");
        }
        if (config.getRabbitPort() > 65535 || config.getRabbitPort() < 0) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitPort should be between 0 and 65535");
        }
        if (StringUtils.isBlank(config.getRabbitHost())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitHost should not be empty");
        }
        if (StringUtils.isBlank(config.getRabbitVirtHost())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitVirtHost should not be empty");
        }
        if (StringUtils.isBlank(config.getUsername())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitUsername should not be empty");
        }
        if (StringUtils.isBlank(config.getPassword())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitPassword should not be empty");
        }
        if (StringUtils.isBlank(config.getNovaExchangeName())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. novaExchangeName should not be empty");
        }
        if (StringUtils.isBlank(config.getCinderExchangeName())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. cinderExchangeName should not be empty");
        }
        if (StringUtils.isBlank(config.getGlanceExchangeName())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. glanceExchangeName should not be empty");
        }
    }

    public static void validateConfig(RabbitNotificationConfigDto config) throws IllegalStateException {
        if (config.getMaxConcurrentConsumers() > 30 || config.getMaxConcurrentConsumers() < 1) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. maxConcurrentConsumers should be between 1 and 30");
        }
        if (config.getMinConcurrentConsumers() > 30 || config.getMinConcurrentConsumers() < 1) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. minConcurrentConsumers should be between 1 and 30");
        }
        if (config.getReplyTimeoutMillis() > 3000000 || config.getReplyTimeoutMillis() < 10000) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. replyTimeoutMillis should be between 10000 and 3000000");
        }
        if (config.getShutdownTimeoutMillis() > 3000000 || config.getShutdownTimeoutMillis() < 10000) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. replyTimeoutMillis should be between 10000 and 3000000");
        }
        if (config.getRabbitPort() > 65535 || config.getRabbitPort() < 0) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitPort should be between 0 and 65535");
        }
        if (StringUtils.isBlank(config.getRabbitHost())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitHost should not be empty");
        }
        if (StringUtils.isBlank(config.getRabbitVirtHost())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitVirtHost should not be empty");
        }
        if (StringUtils.isBlank(config.getRabbitUsername())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitUsername should not be empty");
        }
        if (StringUtils.isBlank(config.getRabbitPassword())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitPassword should not be empty");
        }
        if (CollectionUtils.isEmpty(config.getExchangeName())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. exchangeName should not be empty");
        }
        if (StringUtils.isBlank(config.getRabbitQueue())) {
            throw new IllegalStateException("ERROR: Illegal rabbit configuration. rabbitQueue should not be empty");
        }
    }

}
