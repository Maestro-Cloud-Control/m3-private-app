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

package io.maestro3.agent.admin.factory.impl;

import io.maestro3.agent.admin.IAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkAdminCommand;
import io.maestro3.sdk.v3.model.agent.wizard.SdkPrivateWizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractAdminCommand<REQUEST> implements IAdminCommand<REQUEST> {

    @Override
    public List<REQUEST> getParams(SdkPrivateWizard wizard) {
        if (!wizard.isValid()) {
            throw new IllegalArgumentException("Wizard is invalid, command can't be executed");
        }
        return buildRequests(wizard);
    }

    @Override
    public Map<SdkAdminCommand, REQUEST> prepareCommands(List<REQUEST> params) {
        Map<SdkAdminCommand, REQUEST> sdkAdminCommands = new LinkedHashMap<>();
        for (REQUEST param : params) {
            sdkAdminCommands.put(prepareCommand(param), param);
        }
        return sdkAdminCommands;
    }

    @Override
    public List<SdkAdminCommand> executeCommand(List<REQUEST> params) {
        Map<SdkAdminCommand, REQUEST> commands = prepareCommands(params);
        List<SdkAdminCommand> results = new ArrayList<>();
        boolean success = true;
        for (Map.Entry<SdkAdminCommand, REQUEST> commandParam : commands.entrySet()) {
            String resultMessage;
            if (success) {
                boolean executed = false;
                try {
                    execute(commandParam.getValue());
                    executed = true;
                    resultMessage = "SUCCESS";
                } catch (Exception e) {
                    resultMessage = "FAILED: " + e.getMessage();
                }
                success = executed;
            }else {
                resultMessage = "SKIPPED";
            }
            results.add(commandParam.getKey().setResult(resultMessage).setSuccess(success));
        }
        return results;
    }

    protected List<REQUEST> buildRequests(SdkPrivateWizard wizard) {
        REQUEST request = buildRequest(wizard);
        return Collections.singletonList(request);
    }

    protected abstract REQUEST buildRequest(SdkPrivateWizard wizard);

}
