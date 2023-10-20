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

package io.maestro3.diagnostic.model;


public class JobModel {
    private String jobName;
    private String state;
    private String lastExecutionDate;
    private String nextExecutionDate;

    public JobModel(String jobName, String state, String lastExecutionDate, String nextExecutionDate) {
        this.jobName = jobName;
        this.state = state;
        this.lastExecutionDate = lastExecutionDate;
        this.nextExecutionDate = nextExecutionDate;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLastExecutionDate() {
        return lastExecutionDate;
    }

    public void setLastExecutionDate(String lastExecutionDate) {
        this.lastExecutionDate = lastExecutionDate;
    }

    public String getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(String nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }
}
