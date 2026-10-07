package ru.mkilord.colortomqttapp.core.tracker;

import lombok.extern.log4j.Log4j2;

import java.awt.Color;
import java.util.Properties;

/**
 * Считает изменением евклидово расстояние в RGB больше порога
 * {@code sensitivity} процентов от 255.
 */
@Log4j2
public final class DefaultColorStateTracker extends ColorStateTracker {
    public static final String SENSITIVITY_KEY = "sensitivity";

    private final int sensitivity;

    public DefaultColorStateTracker(Properties props) {
        this.sensitivity = Integer.parseInt(props.getProperty(SENSITIVITY_KEY));
        if (sensitivity < 0 || sensitivity > 100) {
            throw new IllegalArgumentException("Sensitivity must be from 0 to 100");
        }
    }

    @Override
    public boolean hasColorChanged(Color newColor) {
        var current = getCurrentColor();
        var deltaRed = current.getRed() - newColor.getRed();
        var deltaGreen = current.getGreen() - newColor.getGreen();
        var deltaBlue = current.getBlue() - newColor.getBlue();
        var colorDifference = Math.sqrt(deltaRed * deltaRed + deltaGreen * deltaGreen + deltaBlue * deltaBlue);
        var threshold = (255 * sensitivity) / 100.0;
        if (colorDifference > threshold) {
            setCurrentColor(newColor);
            log.debug("Color has change to: {};", newColor);
            return true;
        }
        return false;
    }
}
