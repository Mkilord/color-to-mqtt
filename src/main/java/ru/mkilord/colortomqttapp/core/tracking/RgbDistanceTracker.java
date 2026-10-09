package ru.mkilord.colortomqttapp.core.tracking;

import java.awt.Color;

/**
 * Новый цвет, если расстояние в RGB больше порога в процентах от 255.
 */
public final class RgbDistanceTracker extends ColorChangeTracker {

    private final double threshold;

    public RgbDistanceTracker(int thresholdPercent) {
        this.threshold = 255 * thresholdPercent / 100.0;
    }

    @Override
    protected boolean differs(Color previous, Color next) {
        var dr = previous.getRed() - next.getRed();
        var dg = previous.getGreen() - next.getGreen();
        var db = previous.getBlue() - next.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db) > threshold;
    }
}
