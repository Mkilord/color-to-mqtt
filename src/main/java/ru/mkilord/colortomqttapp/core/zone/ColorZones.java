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
    public static final String WHITE_KELVIN_KEY = "whiteKelvin";
    public static final float DEFAULT_BLACK_THRESHOLD = 5;
    public static final float DEFAULT_GRAY_THRESHOLD = 12;
    public static final int DEFAULT_WHITE_KELVIN = 5000;

    private final float blackThreshold;
    private final float grayThreshold;
    private final int whiteKelvin;

    public ColorZones(float blackThreshold, float grayThreshold) {
        this(blackThreshold, grayThreshold, 0);
    }

    /**
     * @param whiteKelvin температура, с которой отправляется серый; 0 не добавляет ее в сообщение
     */
    public ColorZones(float blackThreshold, float grayThreshold, int whiteKelvin) {
        this.blackThreshold = blackThreshold;
        this.grayThreshold = grayThreshold;
        this.whiteKelvin = whiteKelvin;
    }

    public ColorZones(Properties properties) {
        this(read(properties, BLACK_THRESHOLD_KEY, DEFAULT_BLACK_THRESHOLD),
                read(properties, GRAY_THRESHOLD_KEY, DEFAULT_GRAY_THRESHOLD),
                (int) read(properties, WHITE_KELVIN_KEY, DEFAULT_WHITE_KELVIN));
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
     * У серого корректируется и ограничивается только яркость, тон и насыщенность равны 0.
     */
    public HSBColor toOutput(Color color, ColorModifier modifier, ColorLimit limit) {
        var zone = zoneOf(color);
        if (zone == ColorZone.BLACK) {
            return new HSBColor(0, 0, 0);
        }
        var result = limit.applyFor(modifier.modify(new HSBColor(color)));
        if (zone == ColorZone.GRAY) {
            return new HSBColor(0, 0, result.getBrightness());
        }
        return result;
    }

    /**
     * Температура белого для серого кадра, иначе null.
     */
    public Integer kelvinFor(Color color) {
        return whiteKelvin > 0 && zoneOf(color) == ColorZone.GRAY ? whiteKelvin : null;
    }
}
