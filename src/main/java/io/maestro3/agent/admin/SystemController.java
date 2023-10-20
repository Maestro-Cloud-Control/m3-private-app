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

package io.maestro3.agent.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import io.maestro3.agent.admin.model.AdminSdkResponse;
import io.maestro3.agent.admin.model.DescribeAllRequest;
import io.maestro3.agent.openstack.OpenStackImagesUpdater;
import io.maestro3.agent.util.IPrivateAgentStateUpdater;
import io.maestro3.sdk.internal.M3SdkConstants;
import io.maestro3.sdk.internal.signer.IM3Signer;
import io.maestro3.sdk.internal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static io.maestro3.agent.admin.AdminApiConstants.SYSTEM_ENDPOINT;


@RestController
@RequestMapping(SYSTEM_ENDPOINT)
public class SystemController {

    private final IAdminCommandFactory adminCommandFactory;
    private IPrivateAgentStateUpdater stateUpdater;
    private IM3Signer signer;
    private OpenStackImagesUpdater openStackImagesUpdater;

    @Autowired
    public SystemController(IAdminCommandFactory adminCommandFactory,
                            IPrivateAgentStateUpdater stateUpdater,
                            OpenStackImagesUpdater openStackImagesUpdater,
                            IM3Signer signer) {
        this.adminCommandFactory = adminCommandFactory;
        this.openStackImagesUpdater = openStackImagesUpdater;
        this.stateUpdater = stateUpdater;
        this.signer = signer;
    }

    @PostMapping("/register")
    private AdminSdkResponse registerAgent(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                             @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        Map<String, Object> configDto = JsonUtils.parseJson(decryptedBody, new TypeReference<Map<String, Object>>() {});
        Boolean force = (Boolean) configDto.get("force");
        stateUpdater.pushStateEvent(force == null ? false : force);
        return AdminSdkResponse.of("Agent register request was sent.");
    }

    @PostMapping("/image/sync")
    private AdminSdkResponse syncImages(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                             @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        Map<String, Object> configDto = JsonUtils.parseJson(decryptedBody, new TypeReference<Map<String, Object>>() {});
        Boolean force = (Boolean) configDto.get("force");
        openStackImagesUpdater.updateImages(force == null ? false : force);
        return AdminSdkResponse.of("Images would be updated in some time.");
    }

    @PostMapping("/region/{regionAlias}/tenant/{tenantAlias}/describeAll")
    private AdminSdkResponse enableOrDisableManagement(@RequestHeader(M3SdkConstants.ACCESS_KEY_HEADER) String authKey,
                                                       @PathVariable("tenantAlias") String tenantAlias,
                                                       @PathVariable("regionAlias") String regionAlias,
                                                       @RequestBody String body) {
        String decryptedBody = signer.decrypt(body, authKey);
        IAdminCommand<DescribeAllRequest> command = adminCommandFactory.getCommand(AdminCommandType.OPEN_STACK_SET_TENANT_DESCRIBER_MODE);
        DescribeAllRequest model = command.getParams(decryptedBody, regionAlias, tenantAlias);
        return command.execute(model);
    }

}
