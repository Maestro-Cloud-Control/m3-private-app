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

package io.maestro3.diagnostic.model.healthcheck;

import java.util.Date;
import java.util.List;


public class HealthCheckReport {
    private boolean outdated;
    private Date date;

    private List<RegionStateInfo> orchestratorStates;

    public boolean isOutdated() {
        return outdated;
    }

    public void setOutdated(boolean outdated) {
        this.outdated = outdated;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<RegionStateInfo> getOrchestratorStates() {
        return orchestratorStates;
    }

    public void setOrchestratorStates(List<RegionStateInfo> orchestratorStates) {
        this.orchestratorStates = orchestratorStates;
    }
}
