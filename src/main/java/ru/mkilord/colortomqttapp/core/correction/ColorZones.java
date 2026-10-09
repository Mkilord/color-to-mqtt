package ru.mkilord.colortomqttapp.core.correction;

import ru.mkilord.colortomqttapp.domain.color.HsbColor;
import ru.mkilord.colortomqttapp.domain.settings.DetectionSettings;

import java.awt.Color;

/**
 * Делит цвета на черные, серые и цветные. У почти черного и почти серого пикселя тон
 * определяется шумом, поэтому такие цвета приводятся к чистому черному или серому.
 */
public final class ColorZones {

    private final float blackThreshold;
    private final float grayThreshold;

    public ColorZones(float blackThreshold, float grayThreshold) {
        this.blackThreshold = blackThreshold;
        this.grayThreshold = grayThreshold;
    }

    public static ColorZones of(DetectionSettings settings) {
        return new ColorZones(settings.blackThreshold(), settings.grayThreshold());
    }

    public ColorZone zoneOf(Color color) {
        var hsb = HsbColor.of(color);
        if (hsb.brightness() < blackThreshold) {
            return ColorZone.BLACK;
        }
        if (hsb.saturation() < grayThreshold) {
            return ColorZone.GRAY;
        }
        return ColorZone.COLOR;
    }

    /** Черный становится чистым черным, серый чистым серым той же яркости. */
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
}
