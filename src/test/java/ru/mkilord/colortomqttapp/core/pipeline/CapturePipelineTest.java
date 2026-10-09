package ru.mkilord.colortomqttapp.core.pipeline;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.capture.ScreenCapture;
import ru.mkilord.colortomqttapp.core.correction.ColorCorrector;
import ru.mkilord.colortomqttapp.core.correction.ColorZones;
import ru.mkilord.colortomqttapp.core.correction.LampColorConverter;
import ru.mkilord.colortomqttapp.core.publishing.ColorPublisher;
import ru.mkilord.colortomqttapp.core.tracking.ColorChangeTracker;
import ru.mkilord.colortomqttapp.core.tracking.StabilityGate;
import ru.mkilord.colortomqttapp.domain.color.HsbColor;
import ru.mkilord.colortomqttapp.domain.settings.CorrectionSettings;
import ru.mkilord.colortomqttapp.domain.settings.SendingSettings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CapturePipelineTest {

    private static final Color RED = new Color(220, 20, 20);
    private static final Color GREEN = new Color(20, 200, 40);

    private final long[] clockMs = {0};
    private Color screenColor = Color.BLACK;
    private RuntimeException captureError;
    private final FakePublisher publisher = new FakePublisher();
    private final CapturePipeline pipeline = pipeline(150);

    @Test
    void firstFrameIsSentImmediately() {
        frame();

        assertThat(publisher.sent).containsExactly(HsbColor.BLACK);
        assertThat(pipeline.state().lastPayload()).isEqualTo("{\"hue\":0,\"sat\":0,\"brightness\":0}");
    }

    @Test
    void failedPublishIsRetriedOnNextFrame() {
        publisher.online = false;
        frame();
        publisher.online = true;
        frame();

        assertThat(publisher.sent).hasSize(1);
        assertThat(pipeline.state().lastPayload()).isNotNull();
    }

    @Test
    void stableChangeIsSentOnceAndFlashIsSwallowed() {
        frames(1_000);
        screenColor = RED;
        frames(16);
        screenColor = GREEN;
        frames(1_000);

        assertThat(publisher.sent).hasSize(2);
        assertThat(publisher.sent.get(1).hue()).isBetween(120f, 135f);
    }

    @Test
    void captureErrorIsReportedAndCleared() {
        captureError = new IllegalStateException("нет экрана");
        frame();

        assertThat(pipeline.state().error()).contains("нет экрана");
        assertThat(pipeline.state().errorAt()).isNotNull();

        captureError = null;
        frame();
        assertThat(pipeline.state().error()).isNull();
    }

    private void frame() {
        pipeline.processFrame();
        clockMs[0] += 8;
    }

    private void frames(long durationMs) {
        for (long t = 0; t < durationMs; t += 8) {
            frame();
        }
    }

    private CapturePipeline pipeline(long holdMs) {
        var sending = SendingSettings.DEFAULTS;
        var zones = new ColorZones(5, 12);
        ScreenCapture screen = new ScreenCapture() {
            @Override
            public Dimension screenSize() {
                return new Dimension(100, 100);
            }

            @Override
            public BufferedImage capture(Rectangle area) {
                if (captureError != null) {
                    throw captureError;
                }
                return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            }
        };
        return CapturePipeline.builder()
                .screen(screen, new Rectangle(0, 0, 10, 10))
                .detector(image -> screenColor)
                .zones(zones)
                .tracker(ColorChangeTracker.of(sending))
                .stability(new StabilityGate(ColorChangeTracker.of(sending), zones, holdMs, this::nanos))
                .converter(new LampColorConverter(zones,
                        new ColorCorrector(CorrectionSettings.builder().saturationBoost(0).build())))
                .publisher(publisher)
                .periods(10, 100)
                .clock(this::nanos)
                .build();
    }

    private long nanos() {
        return TimeUnit.MILLISECONDS.toNanos(clockMs[0]);
    }

    private static final class FakePublisher implements ColorPublisher {
        final List<HsbColor> sent = new ArrayList<>();
        boolean online = true;

        @Override
        public boolean publish(HsbColor color) {
            if (online) {
                sent.add(color);
            }
            return online;
        }

        @Override
        public boolean isConnected() {
            return online;
        }

        @Override
        public String lastError() {
            return online ? null : "нет связи";
        }

        @Override
        public void close() {
        }
    }
}
