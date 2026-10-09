package ru.mkilord.colortomqttapp.core;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.correction.ColorZones;
import ru.mkilord.colortomqttapp.core.tracking.ColorChangeTracker;
import ru.mkilord.colortomqttapp.core.tracking.StabilityGate;
import ru.mkilord.colortomqttapp.domain.settings.ComparisonMethod;
import ru.mkilord.colortomqttapp.domain.settings.SendingSettings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingTest {

    private static final Color RED = new Color(220, 20, 20);
    private static final Color GREEN = new Color(20, 200, 40);

    private static ColorChangeTracker tracker(ComparisonMethod method) {
        return ColorChangeTracker.of(SendingSettings.builder().comparison(method).hueTolerance(0.2f).build());
    }

    @Test
    void anyChangeRemembersNewColor() {
        var tracker = tracker(ComparisonMethod.ANY_CHANGE);

        assertThat(tracker.accept(Color.RED)).isTrue();
        assertThat(tracker.current()).isEqualTo(Color.RED);
        assertThat(tracker.accept(Color.RED)).isFalse();
    }

    @Test
    void rgbDistanceIgnoresSmallChanges() {
        var tracker = tracker(ComparisonMethod.RGB_DISTANCE);

        assertThat(tracker.accept(new Color(10, 10, 10))).isFalse();
        assertThat(tracker.current()).isEqualTo(Color.BLACK);
        assertThat(tracker.accept(Color.WHITE)).isTrue();
    }

    @Test
    void hsbToleranceTreatsHueAsCircle() {
        var tracker = tracker(ComparisonMethod.HSB_TOLERANCE);

        assertThat(tracker.accept(Color.getHSBColor(350f / 360, 1f, 1f))).isTrue();
        assertThat(tracker.accept(Color.getHSBColor(5f / 360, 1f, 1f))).isFalse();
        assertThat(tracker.accept(Color.GREEN)).isTrue();
    }

    @Test
    void shortFlashBetweenLongScenesIsSwallowed() {
        var clock = new long[1];
        var gate = new StabilityGate(tracker(ComparisonMethod.HSB_TOLERANCE), new ColorZones(5, 12), 150,
                () -> TimeUnit.MILLISECONDS.toNanos(clock[0]));
        var passed = new ArrayList<Color>();

        run(gate, clock, Color.BLACK, 10_000, passed);
        run(gate, clock, RED, 16, passed);
        run(gate, clock, GREEN, 10_000, passed);

        assertThat(passed).containsExactly(Color.BLACK, GREEN);
    }

    @Test
    void colorHeldLongEnoughPasses() {
        var clock = new long[1];
        var gate = new StabilityGate(tracker(ComparisonMethod.HSB_TOLERANCE), new ColorZones(5, 12), 150,
                () -> TimeUnit.MILLISECONDS.toNanos(clock[0]));
        run(gate, clock, Color.BLACK, 1_000, new ArrayList<>());

        assertThat(gate.isStable(RED)).isFalse();
        clock[0] += 160;
        assertThat(gate.isStable(RED)).isTrue();
    }

    @Test
    void zeroHoldLetsEverythingThrough() {
        var gate = new StabilityGate(tracker(ComparisonMethod.HSB_TOLERANCE), new ColorZones(5, 12), 0, () -> 0L);

        assertThat(gate.isStable(RED)).isTrue();
        assertThat(gate.isStable(GREEN)).isTrue();
    }

    /** Кадры с шагом 8 мс; в {@code passed} попадают прошедшие цвета без повторов подряд. */
    private static void run(StabilityGate gate, long[] clock, Color color, long durationMs, List<Color> passed) {
        for (long t = 0; t < durationMs; t += 8) {
            if (gate.isStable(color) && (passed.isEmpty() || !passed.get(passed.size() - 1).equals(color))) {
                passed.add(color);
            }
            clock[0] += 8;
        }
    }
}
