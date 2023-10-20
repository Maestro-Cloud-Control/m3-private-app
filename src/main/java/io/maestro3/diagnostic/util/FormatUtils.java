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

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;


public final class FormatUtils {

    private FormatUtils() {
        throw new UnsupportedOperationException("Instantiation is forbidden.");
    }

    /**
     * Method for converting day to string. Use "dd-MM-yyyy" format pattern.
     *
     * @param date date to convert.
     * @return day as string.
     */
    public static String getPrettyDate(Date date) {
        return new SimpleDateFormat("dd-MM-yyyy").format(date);
    }

    /**
     * @param date date to convert
     * @return day as string
     */
    public static String getPrettyDate(DateTime date) {
        return new SimpleDateFormat("dd-MM-yyyy").format(date.toDate());
    }

    public static String toString(Date from, Date to) {
        return "[" + from + ";" + to + "]";
    }

    public static String htmlCode(Color color) {
        String s = Integer.toHexString(color.getRGB() & 0xffffff);
        if (s.length() < 6) { // pad on left with zeros
            s = "000000".substring(0, 6 - s.length()) + s;
        }
        return '#' + s;
    }

    /**
     * Method for transforming time delta in milliseconds into hours, minutes, seconds.
     *
     * @param timeDelta time delta in millis
     * @return string containing number and measure systems
     * Method does not not convert into days, years (i.e. if millis is more than one day return millis).
     */
    public static String formatTime(long timeDelta) {
        if (timeDelta >= 0 && timeDelta < 1000) {
            return timeDelta + " millis";
        }
        if (timeDelta >= 1000 && timeDelta < 60000) {
            return timeDelta / 1000 + " second(s) " +
                    timeDelta % 1000 + " millis";
        }
        if (timeDelta >= 60000 && timeDelta < 3600000) {
            return timeDelta / 60000 + " minute(s) " +
                    (timeDelta % 60000) / 1000 + " second(s) " +
                    (timeDelta % 60000) % 1000 + " millis";
        }
        if (timeDelta >= 3600000 && timeDelta < 86400000) {
            return timeDelta / 3600000 + " hr(s) " +
                    ((timeDelta % 3600000) / 60000) + " minute(s) " +
                    (((timeDelta % 3600000)) % 60000) / 1000 + " second(s)";
        }

        return timeDelta + " millis";
    }

    /**
     * Format number inserting "'" after each 3-d digit. Does not affect digits after "." . Example:
     * <ul>
     * <li>123123 - 123'123</li>
     * <li>123.123123 - 123.123123</li>
     * <li>1234 - 1'234</li>
     * <li>123123123 - 123'123'123</li>
     * </ul>
     *
     * @param number value to format
     * @return formatted string
     */
    public static String format(Number number) {
        if (number == null) {
            return StringUtils.EMPTY;
        }
        StringBuilder result = new StringBuilder();

        final String delimiter = "'";
        final int maxDigitsPerGroup = 3;
        String scale = StringUtils.EMPTY;
        String sign = StringUtils.EMPTY;

        String integer = (number instanceof BigDecimal) ? ((BigDecimal) number).toPlainString() : String.valueOf(number);

        //split number into 2 parts: integer and scale
        if (integer.contains(".")) {
            String[] integerWithScale = integer.split("\\.");
            integer = integerWithScale[0];
            scale = "." + integerWithScale[1];
        }

        //get rid of minus operator
        if (integer.startsWith("-")) {
            integer = integer.substring(1, integer.length());
            sign = "-";
        }

        char[] digits = integer.toCharArray();
        int digitsInGroup = 0;
        for (int i = digits.length - 1; i >= 0; i--) {
            digitsInGroup++;
            result.append(digits[i]);
            boolean lastDigit = (i == 0);
            boolean addDelimiter = (digitsInGroup == maxDigitsPerGroup) && !lastDigit;

            if (addDelimiter) {
                result.append(delimiter);
                digitsInGroup = 0;
            }
        }

        result.append(sign).reverse().append(scale);

        return result.toString();
    }

    public static String when(Date date) {
        return "on " + getPrettyDate(date);
    }

    public static String whenHourly(Date date, boolean hourly) {
        if (hourly) {
            return "on " + getPrettyDateHourly(date);
        } else {
            return "on " + getPrettyDate(date);
        }
    }

    public static String getPrettyDateHourly(Date date) {
        return new SimpleDateFormat("HH:mm").format(date);
    }
}
