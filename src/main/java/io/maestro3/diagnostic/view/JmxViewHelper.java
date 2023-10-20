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

package io.maestro3.diagnostic.view;

import io.maestro3.diagnostic.util.ColorUtils;
import io.maestro3.diagnostic.util.DateUtils;
import io.maestro3.diagnostic.util.FormatUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


public class JmxViewHelper {

    public static String period(Long time) {
        return time == null ? null : DateUtils.formatTime(time);
    }

    public static Date date(Long millis) {
        return millis == null ? null : new Date(millis);
    }

    public static Long after(Date date) {
        return date == null ? null : System.currentTimeMillis() - date.getTime();
    }

    public static Long before(Date date) {
        return date == null ? null : date.getTime() - System.currentTimeMillis();
    }

    public static Long after(Long date) {
        return date == null ? null : System.currentTimeMillis() - date;
    }

    public static Long before(Long date) {
        return date == null ? null : date - System.currentTimeMillis();
    }

    public String prettyNumber(Number number) {
        return FormatUtils.format(number);
    }

    public String reformatDouble(String string) {
        Double value = getDouble(string);
        if (value == null) {
            return string;
        }
        return prettyNumber(value);
    }

    public String reformatInteger(String string) {
        Integer value = getInteger(string);
        if (value == null) {
            return string;
        }
        return prettyNumber(value);
    }

    public Double getDouble(String string) {
        try {
            return Double.valueOf(string);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    public Integer getInteger(String string) {
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    public String prettyEnumName(String string) {
        if (StringUtils.isBlank(string)) {
            return "";
        }
        String result = string.toLowerCase(Locale.US).replace("_", " ");
        return StringUtils.capitalize(result);
    }

    public String prettyPeriod(Long time) {
        return time == null ? null : DateUtils.formatTime(time);
    }

    public Long afterDate(Date date) {
        return date == null ? null : System.currentTimeMillis() - date.getTime();
    }

    public Long beforeDate(Date date) {
        return date == null ? null : date.getTime() - System.currentTimeMillis();
    }

    public Date toDate(Long date) {
        return date == null ? null : new Date(date);
    }

    public double position(long start, long end, Long value) {
        if (value == null) {
            return 1.0;
        } else if (value <= start) {
            return 0.0;
        } else if (value >= end) {
            return 1.0;
        }
        return ((double) value - start) / (end - start);
    }

    public String gradient(double position, List<String> htmlColors) {
        List<Color> colors = new LinkedList<>();
        for (String htmlColor : htmlColors) {
            colors.add(ColorUtils.fromHtml(htmlColor));
        }
        return FormatUtils.htmlCode(ColorUtils.gradient(colors.toArray(new Color[0]), position));
    }
}
