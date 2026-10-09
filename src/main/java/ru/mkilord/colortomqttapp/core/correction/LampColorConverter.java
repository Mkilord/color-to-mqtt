package ru.mkilord.colortomqttapp.core.correction;

import ru.mkilord.colortomqttapp.domain.color.HsbColor;

import java.awt.Color;

/**
 * Цвет экрана в цвет для ламп. Черный уходит яркостью 0 без поправок. У серого поправляется
 * только яркость, а насыщенность 1, а не 0: при нуле лампы переходят в режим цветовой
 * температуры со своим оттенком белого.
 */
public final class LampColorConverter {

    public static final float GRAY_SATURATION = 1;

    private final ColorZones zones;
    private final ColorCorrector corrector;

    public LampColorConverter(ColorZones zones, ColorCorrector corrector) {
        this.zones = zones;
        this.corrector = corrector;
    }

    public HsbColor convert(Color color) {
        return switch (zones.zoneOf(color)) {
            case BLACK -> HsbColor.BLACK;
            case GRAY -> new HsbColor(0, GRAY_SATURATION, corrector.apply(HsbColor.of(color)).brightness());
            case COLOR -> corrector.apply(HsbColor.of(color));
        };
    }
}
