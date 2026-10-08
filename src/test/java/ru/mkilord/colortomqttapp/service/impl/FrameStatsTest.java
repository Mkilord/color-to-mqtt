package ru.mkilord.colortomqttapp.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrameStatsTest {

    @Test
    void countsFramesPerSecondAndAverageTimes() {
        var stats = new FrameStats();
        long frame = 20_000_000L;
        for (int i = 0; i <= 50; i++) {
            stats.record(8_000_000L, 500_000L, i * frame);
        }

        var perf = stats.snapshot(false);

        assertThat(perf.fps()).isEqualTo(50.0);
        assertThat(perf.captureMs()).isEqualTo(8.0);
        assertThat(perf.processMs()).isEqualTo(0.5);
        assertThat(perf.idle()).isFalse();
    }
}
