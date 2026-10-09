package ru.mkilord.colortomqttapp.core.tracking;

import java.awt.Color;

/**
 * Новый цвет, если тон, насыщенность или яркость изменились больше допуска. Тон сравнивается по кругу.
 */
public final class HsbToleranceTracker extends ColorChangeTracker {

    private final float hueTolerance;
    private final float saturationTolerance;
    private final float brightnessTolerance;

    public HsbToleranceTracker(float hueTolerance, float saturationTolerance, float brightnessTolerance) {
        this.hueTolerance = hueTolerance;
        this.saturationTolerance = saturationTolerance;
        this.brightnessTolerance = brightnessTolerance;
    }

    @Override
    protected boolean differs(Color previous, Color next) {
        var a = Color.RGBtoHSB(previous.getRed(), previous.getGreen(), previous.getBlue(), null);
        var b = Color.RGBtoHSB(next.getRed(), next.getGreen(), next.getBlue(), null);
        var dh = Math.abs(a[0] - b[0]);
        dh = Math.min(dh, 1 - dh);
        return dh > hueTolerance
                || Math.abs(a[1] - b[1]) > saturationTolerance
                || Math.abs(a[2] - b[2]) > brightnessTolerance;
    }
}
