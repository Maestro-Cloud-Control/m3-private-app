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

package io.maestro3.agent.amqp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.maestro3.agent.amqp.model.SdkRabbitConfiguration;
import io.maestro3.agent.amqp.tracker.IAmqpMessageTracker;
import io.maestro3.agent.api.ApiConstants;
import io.maestro3.agent.api.HeadersValidator;
import io.maestro3.agent.api.handler.IM3ApiHandler;
import io.maestro3.agent.dao.IRegionRepository;
import io.maestro3.agent.exception.ReadableAgentException;
import io.maestro3.agent.model.base.IRegion;
import io.maestro3.sdk.M3SdkVersion;
import io.maestro3.sdk.internal.M3SdkConstants;
import io.maestro3.sdk.internal.signer.IM3Signer;
import io.maestro3.sdk.internal.util.JsonUtils;
import io.maestro3.sdk.v3.core.ActionType;
import io.maestro3.sdk.v3.core.M3ActionParamNames;
import io.maestro3.sdk.v3.core.M3ApiAction;
import io.maestro3.sdk.v3.core.M3BatchResult;
import io.maestro3.sdk.v3.core.M3RawResult;
import io.maestro3.sdk.v3.core.M3Result;
import io.maestro3.sdk.v3.model.SdkCloud;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.security.auth.message.AuthException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
public class RabbitMqM3SdkListener {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitMqM3SdkListener.class);

    private Map<SdkCloud, Map<M3SdkVersion, Map<ActionType, IM3ApiHandler>>> handlers = new HashMap<>();

    private HeadersValidator headersValidator;
    private IM3Signer signer;
    private IAmqpMessageTracker tracker;
    private SdkRabbitConfiguration configuration;
    private IRegionRepository regionDbService;

    @Autowired
    public RabbitMqM3SdkListener(List<IM3ApiHandler> handlers, @Qualifier(ApiConstants.SDK_VALIDATOR) HeadersValidator headersValidator, IM3Signer signer, IRegionRepository regionDbService, IAmqpMessageTracker tracker,
                                 SdkRabbitConfiguration configuration) {
        this.headersValidator = headersValidator;
        this.configuration = configuration;
        this.signer = signer;
        this.tracker = tracker;
        this.regionDbService = regionDbService;
        initHandlers(handlers);
    }

    @RabbitListener(
        queues = "${private.agent.rabbit.m3api.sync.queue}",
        containerFactory = "m3ApiListenerContainerFactory",
        group = "rabbitListeners")
    public String receiveSyncMessage(@Payload String cryptedM3ApiActionsJson,
                                     @Headers Map<String, Object> headers) {
        return processMessage(cryptedM3ApiActionsJson, headers, configuration.getSyncQueue(), false);
    }


    @RabbitListener(
        queues = "${private.agent.rabbit.m3api.async.queue}",
        containerFactory = "m3ApiListenerContainerFactory",
        group = "rabbitListeners")
    public String receiveAsyncMessage(@Payload String cryptedM3ApiActionsJson,
                                      @Headers Map<String, Object> headers) {
        return processMessage(cryptedM3ApiActionsJson, headers, configuration.getAsyncQueue(), true);
    }

    private String processMessage(@Payload String cryptedM3ApiActionsJson, @Headers Map<String, Object> headers, String sourceQueue, boolean async) {
        tracker.trackReceive(PrivateAgentAmqpConstants.SDK_REGION, sourceQueue);
        M3BatchResult responseContainer = new M3BatchResult();
        String accessKey = (String) headers.get(M3SdkConstants.ACCESS_KEY_HEADER);
        try {
            List<M3ApiAction> actions = headersValidator.validateAndDecrypt(headers, cryptedM3ApiActionsJson);
            String version = (String) headers.get(M3SdkConstants.SDK_VERSION_HEADER);
            M3SdkVersion sdkVersion = M3SdkVersion.fromVersion(version);
            for (M3ApiAction m3ApiAction : actions) {
                responseContainer.addResponse(getM3ApiResult(sdkVersion, m3ApiAction));
            }
        } catch (AuthException ex) {
            LOG.error("Failed to process request. Cause: " + ex.getMessage(), ex);
            M3RawResult blocked = M3Result.error("", ex.getMessage());
            responseContainer = new M3BatchResult(blocked);
            return JsonUtils.convertObjectToJson(responseContainer);
        } catch (Exception ex) {
            LOG.error("Failed to process request. Cause: " + ex.getMessage(), ex);
            M3RawResult blocked = M3Result.error("", ex.getMessage());
            responseContainer = new M3BatchResult(blocked);
        }
        if (async) {
            return null;
        }
        String encrypt = signer.encrypt(JsonUtils.convertObjectToJson(responseContainer), accessKey);
        tracker.trackSend(PrivateAgentAmqpConstants.SDK_REGION, configuration.getResponseQueue());
        return encrypt;
    }

    private M3RawResult getM3ApiResult(M3SdkVersion version, M3ApiAction m3ApiAction) {
        M3RawResult m3ApiResult;
        try {
            Map<String, Object> params = m3ApiAction.getParams();
            String body = (String) params.get("body");
            params = JsonUtils.parseJson(body, new TypeReference<Map<String, Object>>() {
            });
            m3ApiAction.getParams().putAll(params);
            String regionAlias = (String) params.get(M3ActionParamNames.REGION);
            IRegion region = regionDbService.findByRegionAlias(regionAlias);
            IM3ApiHandler handler = getHandler(version, m3ApiAction.getType(), region);
            if (handler == null) {
                String message = region == null ?
                    String.format("Region %s is not configured on private agent", regionAlias)
                    : String.format("Failed to find handler for action type '%s'", m3ApiAction.getType());
                LOG.error(message);
                return M3Result.error(m3ApiAction.getId(), message, message);
            }
            LOG.info("Received request for action {} in region {}", m3ApiAction.getType(), regionAlias);
            m3ApiResult = handler.handle(m3ApiAction);
        } catch (ReadableAgentException e) {
            LOG.error("Cannot execute action", e);
            return M3Result.error(m3ApiAction.getId(), e.getMessage(), e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to process request. Cause: " + e.getMessage(), e);
            m3ApiResult = M3Result.error(m3ApiAction.getId(), e.getMessage());
        }
        return m3ApiResult;
    }

    private IM3ApiHandler getHandler(M3SdkVersion sdkVersion, ActionType actionType, IRegion region) {
        Map<M3SdkVersion, Map<ActionType, IM3ApiHandler>> versionActionHandlerMap;
        if (Objects.isNull(region)) {
            versionActionHandlerMap = this.handlers.get(null);
        } else {
            SdkCloud cloud = SdkCloud.fromValue(region.getCloud().name());
            versionActionHandlerMap = this.handlers.get(cloud);
        }
        if (Objects.isNull(sdkVersion)) {
            String message = String.format("Action type '%s' is not supported for this version ", actionType);
            LOG.error(message);
            return null;
        }
        Map<ActionType, IM3ApiHandler> versionedHandlers = versionActionHandlerMap.get(sdkVersion);
        if (MapUtils.isEmpty(versionedHandlers)) {
            return getHandler(sdkVersion.getPrevious(), actionType, region);
        }
        IM3ApiHandler handler = versionedHandlers.get(actionType);
        if (handler == null) {
            Map<ActionType, IM3ApiHandler> actionTypeIM3ApiHandlerMap = versionActionHandlerMap.get(sdkVersion.getPrevious());
            if (actionTypeIM3ApiHandlerMap == null) {
                String message = String.format("Can't find handlers for action %s", actionType);
                LOG.error(message);
                return null;
            }
            return actionTypeIM3ApiHandlerMap.get(actionType);
        }
        return handler;
    }

    private void initHandlers(List<IM3ApiHandler> handlersList) {
        for (IM3ApiHandler handler : handlersList) {
            if (handlers.containsKey(handler.getSupportedCloud())) {
                initVersionMap(handler, handlers.get(handler.getSupportedCloud()));
            } else {
                Map<M3SdkVersion, Map<ActionType, IM3ApiHandler>> versionMap = new HashMap<>();
                handlers.put(handler.getSupportedCloud(), versionMap);
                initVersionMap(handler, versionMap);
            }
        }
    }

    private void initVersionMap(IM3ApiHandler handler, Map<M3SdkVersion, Map<ActionType, IM3ApiHandler>> versionMap) {
        Map<ActionType, IM3ApiHandler> actionMap;
        if (versionMap.containsKey(handler.getSupportedVersion())) {
            actionMap = versionMap.get(handler.getSupportedVersion());
        } else {
            actionMap = new HashMap<>();
            versionMap.put(handler.getSupportedVersion(), actionMap);
        }
        handler.getSupportedActions().forEach(
            action -> actionMap.put(action, handler)
        );
    }
}
