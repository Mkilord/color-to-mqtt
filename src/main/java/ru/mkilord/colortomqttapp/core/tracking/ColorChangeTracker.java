package ru.mkilord.colortomqttapp.core.tracking;

import ru.mkilord.colortomqttapp.domain.settings.SendingSettings;

import java.awt.Color;

/**
 * Помнит последний принятый цвет и решает, достаточно ли отличается новый.
 * Пишет поток захвата, читает веб-запрос, поэтому текущий цвет volatile.
 */
public abstract class ColorChangeTracker {

    private volatile Color current = Color.BLACK;

    public static ColorChangeTracker of(SendingSettings settings) {
        return switch (settings.comparison()) {
            case HSB_TOLERANCE -> new HsbToleranceTracker(settings.hueTolerance(),
                    settings.saturationTolerance(), settings.brightnessTolerance());
            case RGB_DISTANCE -> new RgbDistanceTracker(settings.rgbThresholdPercent());
            case ANY_CHANGE -> new AnyChangeTracker();
        };
    }

    public Color current() {
        return current;
    }

    public void remember(Color color) {
        current = color;
    }

    /**
     * @return true, если цвет считается новым; тогда он и запоминается
     */
    public boolean accept(Color color) {
        if (differs(current, color)) {
            current = color;
            return true;
        }
        return false;
    }

    protected abstract boolean differs(Color previous, Color next);
}
