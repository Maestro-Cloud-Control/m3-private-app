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

package io.maestro3.diagnostic.util;

import io.maestro3.sdk.internal.util.StringUtils;

import java.util.Arrays;
import java.util.List;


public final class DateUtils {

    private static final List<TimeUnit> DEFAULT_UNITS = Arrays.asList(TimeUnit.YEARS, TimeUnit.MONTHS,
            TimeUnit.WEEKS, TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
    private static final String DEFAULT_EMPTY_TIME_DESCRIPTION = "0ms";
    private static final int DEFAULT_UNITS_LIMIT = 2;

    private DateUtils() {
        throw new UnsupportedOperationException("Instantiation is forbidden.");
    }

    //String representation of Time

    public static String formatTime(long millis) {
        return formatTime(millis, DEFAULT_UNITS, DEFAULT_EMPTY_TIME_DESCRIPTION, DEFAULT_UNITS_LIMIT, false);
    }


    private static String formatTime(long millis, List<TimeUnit> units, String emptyDescription, int unitsLimit, boolean useFullLabels) {
        if (millis == 0) {
            return emptyDescription;
        }

        long timeLeft = millis;
        StringBuilder builder = new StringBuilder();
        String prefix = StringUtils.EMPTY_STRING;
        int unitsCount = 0;
        for (TimeUnit unit : units) {
            long count = timeLeft / unit.millis;
            if (count > 0) {
                unitsCount++;
                timeLeft -= count * unit.millis;
                builder.append(prefix).append(count);
                prefix = " ";

                String label;
                if (useFullLabels) {
                    label = count == 1 ? unit.fullSingularLabel : unit.fullPluralLabel;
                    builder.append(" ");
                } else {
                    label = unit.shortLabel;
                }
                builder.append(label);
            }
            if (unitsCount >= unitsLimit) {
                break;
            }
        }

        String description = builder.toString();
        if (StringUtils.isBlank(description)) {
            return emptyDescription;
        }
        return description;
    }

    enum TimeUnit {
        MILLISECONDS(1, "ms", "millis", "millis"),
        SECONDS(1000, "s", "second", "seconds"),
        MINUTES(1000 * 60L, "m", "minute", "minutes"),
        HOURS(1000 * 60 * 60L, 1, "h", "hour", "hours"),
        DAYS(1000 * 60 * 60 * 24L, 24, "d", "day", "days"),
        WEEKS(1000 * 60 * 60 * 24 * 7L, 168, "w", "week", "weeks"),
        MONTHS(1000 * 60 * 60 * 24 * 30L, 730.5, "mth", "month", "months"),
        YEARS(1000 * 60 * 60 * 24 * 365L, 8766, "y", "year", "years");

        private long millis;
        private double hours;
        private String shortLabel;
        private String fullSingularLabel;
        private String fullPluralLabel;

        TimeUnit(long millis, String shortLabel, String fullSingularLabel, String fullPluralLabel) {
            this.millis = millis;
            this.shortLabel = shortLabel;
            this.fullSingularLabel = fullSingularLabel;
            this.fullPluralLabel = fullPluralLabel;
        }

        TimeUnit(long millis, double hours, String shortLabel, String fullSingularLabel, String fullPluralLabel) {
            this.millis = millis;
            this.hours = hours;
            this.shortLabel = shortLabel;
            this.fullSingularLabel = fullSingularLabel;
            this.fullPluralLabel = fullPluralLabel;
        }
    }
}
