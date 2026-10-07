package ru.mkilord.colortomqttapp.core.tracker;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.awt.Color;

import static lombok.AccessLevel.PRIVATE;

/**
 * Помнит последний отправленный цвет и решает, достаточно ли изменился новый.
 * Цвет пишет поток захвата, а читает веб-запрос, поэтому поле volatile.
 */
@Setter
@Getter
@FieldDefaults(level = PRIVATE)
public abstract class ColorStateTracker {
    public static final String STATE_TRACKER_KEY = "stateTracker";

    volatile Color currentColor = Color.BLACK;

    /**
     * @return true, если цвет изменился; новый цвет при этом запоминается
     */
    public abstract boolean hasColorChanged(Color color);
}
