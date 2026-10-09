package ru.mkilord.colortomqttapp.core.tracking;

import ru.mkilord.colortomqttapp.core.correction.ColorZones;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Пропускает цвет дальше, только если он продержался заданное время. Короткая вспышка между
 * двумя долгими сценами (черный, красный на миг, зеленый) иначе заставила бы лампы мигнуть.
 * Одинаковость цвета решает отдельный трекер с теми же правилами, что и при отправке.
 */
public final class StabilityGate {

    private final ColorChangeTracker candidate;
    private final ColorZones zones;
    private final long holdNanos;
    private final LongSupplier nanoClock;
    private long since;

    public StabilityGate(ColorChangeTracker candidate, ColorZones zones, long holdMillis, LongSupplier nanoClock) {
        this.candidate = candidate;
        this.zones = zones;
        this.holdNanos = TimeUnit.MILLISECONDS.toNanos(holdMillis);
        this.nanoClock = nanoClock;
        this.since = nanoClock.getAsLong();
    }

    public boolean isStable(Color color) {
        if (holdNanos <= 0) {
            return true;
        }
        var now = nanoClock.getAsLong();
        var zoneChanged = zones.zoneOf(candidate.current()) != zones.zoneOf(color);
        if (candidate.accept(color) || zoneChanged) {
            candidate.remember(color);
            since = now;
            return false;
        }
        return now - since >= holdNanos;
    }
}
