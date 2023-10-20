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

import org.springframework.util.Assert;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static java.util.Collections.emptyList;


public class ColorUtils {
    private static final long SEED = 1L;
    private static final int MAJOR_COLORS_LIMIT = 6;
    private static final int UNIQUE_TONES = 3;
    private static final String RGB_HEX_COLOR_REGEX = "^[0-9a-fA-F]{6}$";
    private static final String HTML_COLOR_REGEX = "^#[0-9a-fA-F]{6}$";

    public static final int UNIQUE_COLORS = MAJOR_COLORS_LIMIT * UNIQUE_TONES;

    /**
     * Will generate maximum {@value #UNIQUE_COLORS} unique colors. Red color will be always the last one. Calling this
     * method with low and high counts will return same colors.
     *
     * @param count number of colors
     * @return list of colors
     */
    public static List<Color> generateFixed(int count) {
        if (count <= 0) {
            return emptyList();
        }

        int majorColors = MAJOR_COLORS_LIMIT;
        int tones = count / majorColors;
        tones = (count % majorColors != 0) ? tones + 1 : tones; //adjust
        List<Color> colors = new ArrayList<>(count);
        int cursor = 0;
        outerLoop:
        for (int i = 0; i < majorColors; i++) {
            for (int j = 0; j < tones; j++) {
                if (++cursor == count + 1) {
                    break outerLoop;
                }
                float h = (1f / majorColors) * i;
                float s = getSaturation(j);
                float b = getBrightness(j);
                Color color = Color.getHSBColor(h, s, b);
                colors.add(color);
            }
        }

        Color red = colors.remove(0);
        colors.add(red);

        return colors;
    }

    /**
     * Will generate maximum {@value #UNIQUE_COLORS} unique colors. Colors are generated as different as could be.
     * Calling this method with low and high count will generate different colors lists.
     *
     * @param count number of colors
     * @return list of colors
     */
    public static List<Color> generateDistant(int count) {
        return generateDistant(count, 0F);
    }

    /**
     * Will generate maximum {@value #UNIQUE_COLORS} unique colors. Colors are generated as different as could be.
     * Calling this method with low and high count will generate different colors lists.
     *
     * @param count    number of colors
     * @param rotation hue offset
     * @return list of colors
     */
    public static List<Color> generateDistant(int count, float rotation) {
        if (count <= 0) {
            return emptyList();
        }

        int majorColors = MAJOR_COLORS_LIMIT;
        List<Color> colors = new ArrayList<>(count);

        // fill with majors
        for (int tone = 0; tone < count / majorColors; tone++) {
            for (int major = 0; major < majorColors; major++) {
                float hue = normalize((1f / majorColors) * major + rotation);
                float saturation = getSaturation(tone);
                float brightness = getBrightness(tone);
                colors.add(Color.getHSBColor(hue, saturation, brightness));
            }
        }

        // fill the rest with the most different colors
        int restCount = count % majorColors;
        int restTone = count / majorColors;
        float restSaturation = getSaturation(restTone);
        float restBrightness = getBrightness(restTone);
        for (int index = 0; index < restCount; index++) {
            float hue = normalize((1f / restCount) * index + rotation);
            colors.add(Color.getHSBColor(hue, restSaturation, restBrightness));
        }

        return colors;
    }

    private static float normalize(float value) {
        float result = value;
        while (result > 1) {
            result--;
        }
        while (result < 0) {
            result++;
        }
        return result;
    }

    /**
     * Shuffles the list of colors repeating result for repeated input list.
     *
     * @param colors the list of colors to shuffle
     * @return shuffled list of colors
     */
    public static List<Color> shuffle(List<Color> colors) {
        List<Color> result = new LinkedList<>(colors);
        Collections.shuffle(result, new Random(SEED));
        return result;
    }

    private static float getSaturation(int toneIndex) {
        float fullSaturation = 1.0f;
        float decreasedSaturation = 0.5f;
        switch (toneIndex % UNIQUE_TONES) {
            case 0:
            case 1:
                return fullSaturation;
            default:
                return decreasedSaturation;
        }
    }

    private static float getBrightness(int toneIndex) {
        float fullBrightness = 0.8f;
        float decreasedBrightness = 0.45f;
        switch (toneIndex % UNIQUE_TONES) {
            case 0:
                return fullBrightness;
            case 1:
                return decreasedBrightness;
            default:
                return fullBrightness;
        }
    }

    public static Color gradient(Color[] colors, double position) {
        int count = colors.length;
        if (position == 1.0) {
            return colors[count - 1];
        }
        int startIndex = (int) (position * (count - 1));
        Color start = colors[startIndex];
        Color end = colors[startIndex + 1];
        double internalPosition = position - ((double) (startIndex)) / count;
        return new Color(
                slope(start.getRed(), end.getRed(), internalPosition),
                slope(start.getGreen(), end.getGreen(), internalPosition),
                slope(start.getBlue(), end.getBlue(), internalPosition)
        );
    }

    private static int slope(int start, int end, double position) {
        int difference = end - start;
        return start + (int) (difference * position);
    }

    public static Color fromHtml(String color) {
        Assert.hasText(color, "color must not be null or empty");
        Assert.isTrue(color.matches(HTML_COLOR_REGEX), "color must be in html format");
        return fromRgbHex(color.substring(1));
    }

    public static Color fromRgbHex(String color) {
        Assert.hasText(color, "color must not be null or empty");
        Assert.isTrue(color.matches(RGB_HEX_COLOR_REGEX), "color must be in hex RGB format");

        int red = Integer.parseInt(color.substring(0, 2), 16);
        int green = Integer.parseInt(color.substring(2, 4), 16);
        int blue = Integer.parseInt(color.substring(4, 6), 16);
        return new Color(red, green, blue);
    }

    public static String toHtml(Color color) {
        String string = Integer.toHexString(color.getRGB() & 0xffffff);
        if (string.length() < 6) {
            string = "000000".substring(0, 6 - string.length()) + string;
        }
        return '#' + string;
    }

    public static Color desaturate(Color color, float intensity) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        hsb[1] = hsb[1] * (1F - intensity);
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    public static Color highlight(Color color, float intensity) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        hsb[2] = hsb[2] + (1F - hsb[2]) * intensity;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    public static class ColorProcessor {
        private final List<Color> colors;
        private Float desaturation;
        private Float highlighting;

        private ColorProcessor(List<Color> colors) {
            this.colors = colors;
        }

        public static ColorProcessor colorProcessor(List<Color> colors) {
            return new ColorProcessor(colors);
        }

        public static ColorProcessor colorProcessor(int count) {
            return new ColorProcessor(ColorUtils.generateDistant(count));
        }

        public static ColorProcessor colorProcessor(int count, float rotation) {
            return new ColorProcessor(ColorUtils.generateDistant(count, rotation));
        }

        public ColorProcessor desaturate(float intensity) {
            this.desaturation = intensity;
            return this;
        }

        public ColorProcessor highlight(float intensity) {
            this.highlighting = intensity;
            return this;
        }

        public List<Color> process() {
            List<Color> processedColors = new LinkedList<>();
            for (Color color : colors) {
                Color processedColor = new Color(color.getRGB());
                if (desaturation != null) {
                    processedColor = ColorUtils.desaturate(color, desaturation);
                }
                if (highlighting != null) {
                    processedColor = ColorUtils.highlight(color, highlighting);
                }
                processedColors.add(processedColor);
            }
            return processedColors;
        }
    }
}
