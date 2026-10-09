package ru.mkilord.colortomqttapp.core.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mkilord.colortomqttapp.core.capture.ScreenCapture;
import ru.mkilord.colortomqttapp.core.correction.ColorZones;
import ru.mkilord.colortomqttapp.core.correction.LampColorConverter;
import ru.mkilord.colortomqttapp.core.detection.ColorDetector;
import ru.mkilord.colortomqttapp.core.publishing.ColorPayload;
import ru.mkilord.colortomqttapp.core.publishing.ColorPublisher;
import ru.mkilord.colortomqttapp.core.tracking.ColorChangeTracker;
import ru.mkilord.colortomqttapp.core.tracking.StabilityGate;

import java.awt.Color;
import java.awt.Rectangle;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Цикл захвата: снимок, цвет кадра, проверка вспышек и изменений, перевод в цвет ламп, отправка.
 * Работает в своем потоке; экземпляр одноразовый, после {@link #stop()} не перезапускается.
 */
public final class CapturePipeline {

    private static final Logger log = LoggerFactory.getLogger(CapturePipeline.class);

    private final ScreenCapture screen;
    private final Rectangle area;
    private final ColorDetector detector;
    private final ColorZones zones;
    private final ColorChangeTracker tracker;
    private final StabilityGate stability;
    private final LampColorConverter converter;
    private final ColorPublisher publisher;
    private final long framePeriodMs;
    private final long idlePeriodMs;
    private final LongSupplier nanoClock;

    private final ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor(runnable -> {
        var thread = new Thread(runnable, "color-capture");
        thread.setDaemon(true);
        return thread;
    });
    private final FrameStats stats = new FrameStats();
    private final IdleTracker idleTracker = new IdleTracker();

    /** Следующий кадр уходит без проверок: первый кадр и кадр после неудачной отправки. */
    private boolean resend = true;
    private volatile boolean idle;
    private volatile Event captureFailure;
    private volatile Event lastSent;

    private record Event(String text, Instant at) {
    }

    private CapturePipeline(Builder builder) {
        screen = Objects.requireNonNull(builder.screen, "screen");
        area = Objects.requireNonNull(builder.area, "area");
        detector = Objects.requireNonNull(builder.detector, "detector");
        zones = Objects.requireNonNull(builder.zones, "zones");
        tracker = Objects.requireNonNull(builder.tracker, "tracker");
        stability = Objects.requireNonNull(builder.stability, "stability");
        converter = Objects.requireNonNull(builder.converter, "converter");
        publisher = Objects.requireNonNull(builder.publisher, "publisher");
        framePeriodMs = builder.framePeriodMs;
        idlePeriodMs = Math.max(builder.framePeriodMs, builder.idlePeriodMs);
        nanoClock = builder.nanoClock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void start() {
        loop.execute(this::tick);
    }

    public void stop() {
        loop.shutdownNow();
        publisher.close();
    }

    public ColorPublisher publisher() {
        return publisher;
    }

    public PipelineState state() {
        var failure = captureFailure;
        var error = failure != null ? failure.text() : publisher.lastError();
        var sent = lastSent;
        return new PipelineState(tracker.current(), publisher.isConnected(),
                sent == null ? null : sent.text(), sent == null ? null : sent.at(),
                error, failure == null ? null : failure.at(),
                stats.snapshot(idle && idlePeriodMs > framePeriodMs));
    }

    private void tick() {
        processFrame();
        try {
            loop.schedule(this::tick, idle ? idlePeriodMs : framePeriodMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException stopped) {
            log.debug("Захват остановлен");
        }
    }

    void processFrame() {
        try {
            var start = nanoClock.getAsLong();
            var image = screen.capture(area);
            var captured = nanoClock.getAsLong();
            var detected = detector.detect(image);
            idle = idleTracker.update(detected, captured);
            captureFailure = null;
            send(zones.normalize(detected));
            var end = nanoClock.getAsLong();
            stats.record(captured - start, end - captured, end);
        } catch (RuntimeException e) {
            if (captureFailure == null) {
                log.error("Ошибка захвата экрана", e);
            }
            captureFailure = new Event("Ошибка захвата экрана: " + e.getMessage(), Instant.now());
        }
    }

    private void send(Color color) {
        if (!resend && !(stability.isStable(color) && changed(color))) {
            return;
        }
        tracker.remember(color);
        var lampColor = converter.convert(color);
        resend = !publisher.publish(lampColor);
        if (!resend) {
            lastSent = new Event(ColorPayload.of(lampColor), Instant.now());
        }
    }

    /**
     * Переход между черным, серым и цветным считается изменением независимо от допусков:
     * иначе лампы не погаснут при плавном затемнении экрана.
     */
    private boolean changed(Color color) {
        var zoneChanged = zones.zoneOf(tracker.current()) != zones.zoneOf(color);
        return tracker.accept(color) || zoneChanged;
    }

    public static final class Builder {
        private ScreenCapture screen;
        private Rectangle area;
        private ColorDetector detector;
        private ColorZones zones;
        private ColorChangeTracker tracker;
        private StabilityGate stability;
        private LampColorConverter converter;
        private ColorPublisher publisher;
        private long framePeriodMs = 10;
        private long idlePeriodMs;
        private LongSupplier nanoClock = System::nanoTime;

        private Builder() {
        }

        public Builder screen(ScreenCapture value, Rectangle captureArea) {
            screen = value;
            area = captureArea;
            return this;
        }

        public Builder detector(ColorDetector value) {
            detector = value;
            return this;
        }

        public Builder zones(ColorZones value) {
            zones = value;
            return this;
        }

        public Builder tracker(ColorChangeTracker value) {
            tracker = value;
            return this;
        }

        public Builder stability(StabilityGate value) {
            stability = value;
            return this;
        }

        public Builder converter(LampColorConverter value) {
            converter = value;
            return this;
        }

        public Builder publisher(ColorPublisher value) {
            publisher = value;
            return this;
        }

        public Builder periods(long frameMs, long idleMs) {
            framePeriodMs = frameMs;
            idlePeriodMs = idleMs;
            return this;
        }

        public Builder clock(LongSupplier value) {
            nanoClock = value;
            return this;
        }

        public CapturePipeline build() {
            return new CapturePipeline(this);
        }
    }
}
