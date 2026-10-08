package ru.mkilord.colortomqttapp.service.impl;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class IdleTrackerTest {

    private static final long SECOND = 1_000_000_000L;

    @Test
    void becomesIdleAfterThreeSecondsWithoutChange() {
        var tracker = new IdleTracker();
        var gray = new Color(100, 100, 100);

        assertThat(tracker.update(gray, 0)).isFalse();
        assertThat(tracker.update(new Color(102, 99, 101), 2 * SECOND)).isFalse();
        assertThat(tracker.update(gray, 3 * SECOND)).isTrue();
    }

    @Test
    void anyRealChangeWakesUp() {
        var tracker = new IdleTracker();
        tracker.update(Color.BLACK, 0);
        assertThat(tracker.update(Color.BLACK, 5 * SECOND)).isTrue();

        assertThat(tracker.update(new Color(40, 0, 0), 5 * SECOND + 1)).isFalse();
    }
}
