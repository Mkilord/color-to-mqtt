package ru.mkilord.colortomqttapp.core.tracker;

import java.awt.Color;
import java.util.Objects;

/**
 * Считает изменением любое отличие цвета.
 */
public final class SimpleColorStateTracker extends ColorStateTracker {

    @Override
    public boolean hasColorChanged(Color color) {
        var hasChanged = !Objects.equals(getCurrentColor(), color);
        if (hasChanged) {
            setCurrentColor(color);
        }
        return hasChanged;
    }
}
