package ru.mkilord.colortomqttapp.core.tracker;

import ru.mkilord.colortomqttapp.core.zone.ColorZones;

import java.awt.Color;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Пропускает цвет дальше, только если он держится на экране не меньше заданного времени.
 * <p>
 * Короткая вспышка между двумя долгими сценами (черный, красный на миг, зеленый) иначе
 * заставила бы лампы мигнуть. Цвета считаются одинаковыми по тем же правилам, что и при
 * решении об отправке: тем же трекером и с учетом черного, серого и цветного.
 */
public final class StabilityGate {

    public static final String HOLD_TIME_KEY = "holdTime";
    public static final int DEFAULT_HOLD_TIME = 150;

    private final ColorStateTracker candidate;
    private final ColorZones zones;
    private final long holdNanos;
    private final LongSupplier nanoClock;
    private long since;

    /**
     * @param candidate отдельный экземпляр трекера, он помнит цвет-кандидат
     * @param holdMillis сколько цвет должен продержаться; 0 отключает задержку
     * @param nanoClock источник времени в наносекундах
     */
    public StabilityGate(ColorStateTracker candidate, ColorZones zones, long holdMillis, LongSupplier nanoClock) {
        this.candidate = candidate;
        this.zones = zones;
        this.holdNanos = TimeUnit.MILLISECONDS.toNanos(holdMillis);
        this.nanoClock = nanoClock;
        this.since = nanoClock.getAsLong();
    }

    public static long holdMillis(Properties properties) {
        var value = properties.getProperty(HOLD_TIME_KEY);
        return value == null || value.isBlank() ? DEFAULT_HOLD_TIME : Long.parseLong(value.trim());
    }

    /**
     * @return true, если цвет кадра держится не меньше заданного времени
     */
    public boolean isStable(Color color) {
        if (holdNanos <= 0) {
            return true;
        }
        var now = nanoClock.getAsLong();
        var zoneChanged = zones.zoneOf(candidate.getCurrentColor()) != zones.zoneOf(color);
        var colorChanged = candidate.hasColorChanged(color);
        if (colorChanged || zoneChanged) {
            candidate.setCurrentColor(color);
            since = now;
            return false;
        }
        return now - since >= holdNanos;
    }
}
