package ru.mkilord.colortomqttapp.core.zone;

import ru.mkilord.colortomqttapp.core.HSBColor;
import ru.mkilord.colortomqttapp.core.limit.ColorLimit;
import ru.mkilord.colortomqttapp.core.modifier.ColorModifier;

import java.awt.Color;
import java.util.Properties;

/**
 * Делит цвета на черные, серые и цветные.
 * <p>
 * У почти черного и почти серого пикселя тон и насыщенность определяются шумом,
 * поэтому такие цвета приводятся к чистому черному или серому до сравнения и отправки.
 * Пороги задаются в процентах; 0 отключает соответствующую зону.
 */
public final class ColorZones {

    public static final String BLACK_THRESHOLD_KEY = "blackThreshold";
    public static final String GRAY_THRESHOLD_KEY = "grayThreshold";
    public static final float DEFAULT_BLACK_THRESHOLD = 5;
    public static final float DEFAULT_GRAY_THRESHOLD = 12;
    /**
     * Насыщенность, с которой уходит серый. Не 0: при нулевой насыщенности лампы
     * переключаются в режим цветовой температуры со своим оттенком белого.
     */
    public static final float GRAY_SATURATION = 1;

    private final float blackThreshold;
    private final float grayThreshold;

    public ColorZones(float blackThreshold, float grayThreshold) {
        this.blackThreshold = blackThreshold;
        this.grayThreshold = grayThreshold;
    }

    public ColorZones(Properties properties) {
        this(read(properties, BLACK_THRESHOLD_KEY, DEFAULT_BLACK_THRESHOLD),
                read(properties, GRAY_THRESHOLD_KEY, DEFAULT_GRAY_THRESHOLD));
    }

    private static float read(Properties properties, String key, float defaultValue) {
        var value = properties.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : Float.parseFloat(value);
    }

    public ColorZone zoneOf(Color color) {
        var hsb = new HSBColor(color);
        if (hsb.getBrightness() < blackThreshold) {
            return ColorZone.BLACK;
        }
        if (hsb.getSaturation() < grayThreshold) {
            return ColorZone.GRAY;
        }
        return ColorZone.COLOR;
    }

    /**
     * Черный становится {@link Color#BLACK}, серый становится чистым серым той же яркости.
     */
    public Color normalize(Color color) {
        return switch (zoneOf(color)) {
            case BLACK -> Color.BLACK;
            case GRAY -> {
                var v = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
                yield new Color(v, v, v);
            }
            case COLOR -> color;
        };
    }

    /**
     * Цвет для отправки. Черный уходит как 0, 0, 0 без коррекции и ограничений.
     * У серого корректируется и ограничивается только яркость, тон 0, насыщенность {@link #GRAY_SATURATION}.
     */
    public HSBColor toOutput(Color color, ColorModifier modifier, ColorLimit limit) {
        var zone = zoneOf(color);
        if (zone == ColorZone.BLACK) {
            return new HSBColor(0, 0, 0);
        }
        var result = limit.applyFor(modifier.modify(new HSBColor(color)));
        if (zone == ColorZone.GRAY) {
            return new HSBColor(0, GRAY_SATURATION, result.getBrightness());
        }
        return result;
    }
}
