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

package io.maestro3.diagnostic.service.impl;

import io.maestro3.agent.scheduler.IScheduler;
import io.maestro3.diagnostic.model.JobModel;
import io.maestro3.diagnostic.service.IScheduleInfoProvider;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Service
public class ScheduleInfoProvider implements SchedulingConfigurer, IScheduleInfoProvider {

    private static final SimpleTriggerContext TRIGGER_CONTEXT = new SimpleTriggerContext();
    private static final int CRON = 1;
    private static final int FIXED_DELAY = 2;
    private static final int FIXED_RATE = 3;

    private ScheduledTaskRegistrar taskRegistrar;
    private Map<String, IScheduler> schedulerMap;

    public ScheduleInfoProvider(Set<IScheduler> schedulerSet) {
        this.schedulerMap = new HashMap<>();
        for (IScheduler scheduler : schedulerSet) {
            this.schedulerMap.put(scheduler.getClass().getSimpleName(), scheduler);
        }
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.taskRegistrar = taskRegistrar;
    }

    public List<JobModel> getScheduleInfo() {
        List<JobModel> result = new ArrayList<>();
        taskRegistrar.getCronTaskList().forEach(
                task -> result.add(process(task, CRON))
        );
        taskRegistrar.getFixedDelayTaskList().forEach(
                task -> result.add(process(task, FIXED_DELAY))
        );
        taskRegistrar.getFixedRateTaskList().forEach(
                task -> result.add(process(task, FIXED_DELAY))
        );
        result.removeIf(Objects::isNull);
        return result;
    }

    private JobModel process(Task task, int type) {
        IScheduler scheduler = schedulerMap.get(getRunnableClassName(task.getRunnable()));
        if (scheduler == null) {
            return null;
        }
        long lastExecutionStart = scheduler.getLastExecutionStart();
        long lastExecutionEnd = scheduler.getLastExecutionEnd();
        String scheduleTitle = scheduler.getScheduleTitle();
        String nextExecution;
        String lastExecution = lastExecutionStart == 0
                ? "Not executed"
                : new Date(lastExecutionStart).toString();
        switch (type) {
            case CRON:
                CronTask cronTask = (CronTask) task;
                Date execution = cronTask.getTrigger().nextExecutionTime(TRIGGER_CONTEXT);
                nextExecution = execution == null ? "undefined" : execution.toString();
                break;
            case FIXED_DELAY:
                IntervalTask intervalTask = (IntervalTask) task;
                if (lastExecutionStart > lastExecutionEnd) {
                    nextExecution = "After " + intervalTask.getInterval() / 1000 + " seconds after the end of the current task";
                } else {
                    nextExecution = new Date(lastExecutionEnd + intervalTask.getInterval()).toString();
                }
                break;
            case FIXED_RATE:
                IntervalTask fixedRateTask = (IntervalTask) task;
                nextExecution = new Date(lastExecutionStart + fixedRateTask.getInterval()).toString();
                break;
            default:
                nextExecution = "Not calculated yet";
        }
        return new JobModel(
                scheduleTitle,
                resolveState(lastExecutionStart, lastExecutionEnd),
                lastExecution,
                nextExecution);
    }

    private String getRunnableClassName(Runnable runnable) {
        String fullNameWithMethod = runnable.toString();
        String[] split = fullNameWithMethod.split("\\.");
        return split[split.length - 2];
    }

    private String resolveState(long start, long end) {
        return start > end ? "In progress" : "Pending";
    }
}

