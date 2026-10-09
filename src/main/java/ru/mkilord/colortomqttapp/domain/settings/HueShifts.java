package ru.mkilord.colortomqttapp.domain.settings;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

/**
 * Поправка тона для шести опорных цветов, градусы. Между ними поправка меняется линейно.
 */
public record HueShifts(float red, float yellow, float green, float cyan, float blue, float magenta) {

    public static final HueShifts NONE = new HueShifts(0, 0, 0, 0, 0, 0);
    public static final float LIMIT = 60;

    public HueShifts {
        Checks.of("correction.hueShifts")
                .range("red", red, -LIMIT, LIMIT)
                .range("yellow", yellow, -LIMIT, LIMIT)
                .range("green", green, -LIMIT, LIMIT)
                .range("cyan", cyan, -LIMIT, LIMIT)
                .range("blue", blue, -LIMIT, LIMIT)
                .range("magenta", magenta, -LIMIT, LIMIT)
                .validate();
    }

    /** По порядку опорных тонов 0, 60, 120, 180, 240, 300. */
    public float[] toArray() {
        return new float[]{red, yellow, green, cyan, blue, magenta};
    }
}
