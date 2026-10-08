package ru.mkilord.colortomqttapp.core.tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.TestProperties;
import ru.mkilord.colortomqttapp.core.zone.ColorZones;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class StabilityGateTest {

    private static final Color RED = new Color(220, 20, 20);
    private static final Color GREEN = new Color(20, 200, 40);

    private long nowMillis;
    private StabilityGate gate;

    @BeforeEach
    void setUp() {
        nowMillis = 0;
        gate = new StabilityGate(new ToleranceColorStateTracker(TestProperties.defaults()), new ColorZones(5, 12),
                150, () -> TimeUnit.MILLISECONDS.toNanos(nowMillis));
    }

    /**
     * Прогоняет кадры с шагом 8 мс и возвращает цвета, которые прошли бы дальше (без повторов подряд).
     */
    private List<Color> run(Color color, long durationMillis, List<Color> passed) {
        for (long t = 0; t < durationMillis; t += 8) {
            if (gate.isStable(color) && (passed.isEmpty() || !passed.get(passed.size() - 1).equals(color))) {
                passed.add(color);
            }
            nowMillis += 8;
        }
        return passed;
    }

    @Test
    void shortFlashBetweenLongScenesIsSwallowed() {
        var passed = new ArrayList<Color>();
        run(Color.BLACK, 10_000, passed);
        run(RED, 16, passed);
        run(GREEN, 10_000, passed);

        assertThat(passed).containsExactly(Color.BLACK, GREEN);
    }

    @Test
    void colorHeldLongerThanHoldTimePasses() {
        var passed = new ArrayList<Color>();
        run(Color.BLACK, 1_000, passed);
        run(RED, 300, passed);
        run(GREEN, 1_000, passed);

        assertThat(passed).containsExactly(Color.BLACK, RED, GREEN);
    }

    @Test
    void newColorWaitsForHoldTime() {
        run(Color.BLACK, 1_000, new ArrayList<>());

        assertThat(gate.isStable(RED)).isFalse();
        nowMillis += 100;
        assertThat(gate.isStable(RED)).isFalse();
        nowMillis += 60;
        assertThat(gate.isStable(RED)).isTrue();
    }

    @Test
    void zeroHoldTimeLetsEverythingThrough() {
        var noHold = new StabilityGate(new ToleranceColorStateTracker(TestProperties.defaults()), new ColorZones(5, 12),
                0, () -> 0L);

        assertThat(noHold.isStable(RED)).isTrue();
        assertThat(noHold.isStable(GREEN)).isTrue();
    }

    @Test
    void holdTimeDefaultsWhenMissing() {
        assertThat(StabilityGate.holdMillis(new Properties())).isEqualTo(StabilityGate.DEFAULT_HOLD_TIME);
    }
}
