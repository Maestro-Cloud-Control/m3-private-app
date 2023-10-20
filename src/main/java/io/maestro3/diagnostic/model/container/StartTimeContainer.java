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

package io.maestro3.diagnostic.model.container;

import io.maestro3.agent.model.base.InstanceRunRecord;

import java.util.concurrent.TimeUnit;


public class StartTimeContainer {
    private int total;
    private int lessOneMin;
    private int oneToThree;
    private int threeToTen;
    private int tenToThirty ;
    private int moreThenThirty;
    private int error;

    public void update(InstanceRunRecord record){
        total++;
        long startDuration = record.getStartDuration();
        if (startDuration < TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES)){
            lessOneMin++;
            return;
        }
        if (startDuration < TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES)){
            oneToThree++;
            return;
        }
        if (startDuration < TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES)){
            threeToTen++;
            return;
        }
        if (startDuration < TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)){
            tenToThirty++;
            return;
        }
        moreThenThirty++;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getLessOneMin() {
        return lessOneMin;
    }

    public void setLessOneMin(int lessOneMin) {
        this.lessOneMin = lessOneMin;
    }

    public int getOneToThree() {
        return oneToThree;
    }

    public void setOneToThree(int oneToThree) {
        this.oneToThree = oneToThree;
    }

    public int getThreeToTen() {
        return threeToTen;
    }

    public void setThreeToTen(int threeToTen) {
        this.threeToTen = threeToTen;
    }

    public int getTenToThirty() {
        return tenToThirty;
    }

    public void setTenToThirty(int tenToThirty) {
        this.tenToThirty = tenToThirty;
    }

    public int getMoreThenThirty() {
        return moreThenThirty;
    }

    public void setMoreThenThirty(int moreThenThirty) {
        this.moreThenThirty = moreThenThirty;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }
}
