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

import io.maestro3.diagnostic.model.ScheduleOperation;
import io.maestro3.diagnostic.service.IScheduleProcessorInvoker;
import io.maestro3.diagnostic.util.MaestroThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.InvalidParameterException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Service
public class ScheduleProcessorInvoker implements IScheduleProcessorInvoker {

    private final ThreadPoolExecutor callExecutor =
            //standard single pool executor, we need access to #getActiveCount() method
            new ThreadPoolExecutor(1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new MaestroThreadFactory("ui-schedule-invoker"));

    @Autowired
    ScheduleProcessorInvoker() {
    }

    @Override
    public void invoke(ScheduleOperation operation, boolean async) throws InvalidParameterException {
        if (operation == null) {
            throw new InvalidParameterException("Cannot find schedule operation with name: " + operation);
        }

        // ONLY ONE SCHEDULER PER TIME CAN BE EXECUTED VIA THIS POINT !!!
        if (callExecutor.getActiveCount() != 0) {
            throw new InvalidParameterException(" Cannot execute " + operation + " scheduler. Other scheduler IN PROGRESS.");
        }
        // do processing
        Runnable runnableTask;
        switch (operation) {
            case MOCK:
                runnableTask = () -> {};
                break;
            default:
                throw new InvalidParameterException("Operation is not supported for manual invocation: " +
                        operation + ". Allowed schedulers: " + ScheduleOperation.getAllScheduleOperations());
        }

        if (async) {
            callExecutor.execute(runnableTask);
        } else {
            runnableTask.run();
        }
    }
}
