package ru.mkilord.colortomqttapp.service.impl;

import java.awt.Color;

/**
 * Замечает, что экран давно не меняется. В покое захват можно делать реже:
 * на статичном рабочем столе это снимает почти всю нагрузку на процессор.
 */
final class IdleTracker {

    static final long IDLE_AFTER_NANOS = 3_000_000_000L;
    /** Изменение канала меньше этого считается шумом. */
    static final int NOISE = 4;

    private Color last;
    private long lastActivity;

    /**
     * @return true, если цвет кадра не менялся дольше {@link #IDLE_AFTER_NANOS}
     */
    boolean update(Color color, long now) {
        if (last == null || differs(last, color)) {
            lastActivity = now;
        }
        last = color;
        return now - lastActivity >= IDLE_AFTER_NANOS;
    }

    private static boolean differs(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) > NOISE
                || Math.abs(a.getGreen() - b.getGreen()) > NOISE
                || Math.abs(a.getBlue() - b.getBlue()) > NOISE;
    }
}
