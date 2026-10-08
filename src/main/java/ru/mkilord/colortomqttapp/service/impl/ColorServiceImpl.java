package ru.mkilord.colortomqttapp.service.impl;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ru.mkilord.colortomqttapp.core.AbstractFactory;
import ru.mkilord.colortomqttapp.core.detector.ColorDetector;
import ru.mkilord.colortomqttapp.core.limit.ColorLimit;
import ru.mkilord.colortomqttapp.core.limit.DefaultColorLimit;
import ru.mkilord.colortomqttapp.core.modifier.ColorModifier;
import ru.mkilord.colortomqttapp.core.modifier.DefaultColorModifier;
import ru.mkilord.colortomqttapp.core.publisher.ColorPublisher;
import ru.mkilord.colortomqttapp.core.publisher.MQTTColorPublisher;
import ru.mkilord.colortomqttapp.core.screenshoter.DefaultScreenShooter;
import ru.mkilord.colortomqttapp.core.screenshoter.ScreenShooter;
import ru.mkilord.colortomqttapp.core.tracker.ColorStateTracker;
import ru.mkilord.colortomqttapp.core.tracker.StabilityGate;
import ru.mkilord.colortomqttapp.core.zone.ColorZones;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.ColorStatus;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.awt.Color;
import java.time.Instant;
import java.util.Properties;

/**
 * Цикл захвата: снимок области, средний цвет, проверка изменения, сдвиг, ограничения, публикация.
 * Все компоненты цикла создаются при запуске по актуальным настройкам и закрываются при остановке.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {

    private final SettingsService settingsService;

    private Pipeline pipeline;
    private volatile Failure startFailure;

    @Override
    public synchronized boolean isStart() {
        return pipeline != null;
    }

    @Override
    public synchronized void start() {
        if (pipeline != null) {
            return;
        }
        try {
            pipeline = new Pipeline(settingsService.loadOrElseLoadDefault());
        } catch (RuntimeException e) {
            startFailure = new Failure("Не удалось запустить захват: " + e.getMessage(), Instant.now());
            throw e;
        }
        startFailure = null;
        pipeline.start();
        log.info("Захват цвета запущен");
    }

    @Override
    @PreDestroy
    public synchronized void stop() {
        if (pipeline == null) {
            return;
        }
        pipeline.stop();
        pipeline = null;
        log.info("Захват цвета остановлен");
    }

    @Override
    public synchronized void restartIfRunning() {
        if (pipeline != null) {
            stop();
            start();
        }
    }

    @Override
    public synchronized Color getCurrentColor() {
        return pipeline == null ? Color.BLACK : pipeline.tracker.getCurrentColor();
    }

    @Override
    public synchronized ColorStatus getStatus() {
        if (pipeline == null) {
            var settings = settingsService.loadOrElseLoadDefault();
            var failure = startFailure;
            return new ColorStatus(false, toHex(Color.BLACK), settings.getProperty("broker"),
                    settings.getProperty("topic"), null, null, null,
                    failure == null ? null : failure.message(), failure == null ? null : failure.at(), null);
        }
        return pipeline.status();
    }

    static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private record Failure(String message, Instant at) {
    }

    private record Sent(String payload, Instant at) {
    }

    private static final class Pipeline {
        private final Properties properties;
        private final ScreenShooter screenShooter;
        private final ColorDetector detector;
        private final ColorStateTracker tracker;
        private final ColorModifier modifier;
        private final ColorLimit limit;
        private final ColorZones zones;
        private final StabilityGate stability;
        private final ColorPublisher publisher;
        private final RepeatServiceImpl repeater;
        private final FrameStats stats = new FrameStats();
        private final IdleTracker idleTracker = new IdleTracker();
        private final long updatePeriod;
        private final long idlePeriod;
        private volatile boolean idle;

        private volatile Failure captureFailure;
        private volatile Sent lastSent;
        private boolean firstFrame = true;

        Pipeline(Properties properties) {
            this.properties = properties;
            this.screenShooter = new DefaultScreenShooter(properties);
            this.detector = new AbstractFactory<ColorDetector>().get(ColorDetector.DETECTOR_KEY, properties);
            this.tracker = new AbstractFactory<ColorStateTracker>().get(ColorStateTracker.STATE_TRACKER_KEY, properties);
            this.modifier = new DefaultColorModifier(properties);
            this.limit = new DefaultColorLimit(properties);
            this.zones = new ColorZones(properties);
            this.stability = new StabilityGate(
                    new AbstractFactory<ColorStateTracker>().get(ColorStateTracker.STATE_TRACKER_KEY, properties),
                    zones, StabilityGate.holdMillis(properties), System::nanoTime);
            this.publisher = new MQTTColorPublisher(properties);
            this.repeater = new RepeatServiceImpl(properties);
            this.updatePeriod = Long.parseLong(properties.getProperty("updatePeriod"));
            this.idlePeriod = Long.parseLong(properties.getProperty(IDLE_PERIOD_KEY, String.valueOf(DEFAULT_IDLE_PERIOD)));
        }

        void start() {
            repeater.repeat(this::processFrame, () -> idle ? Math.max(updatePeriod, idlePeriod) : updatePeriod);
        }

        void stop() {
            repeater.stop();
            publisher.close();
        }

        ColorStatus status() {
            var failure = captureFailure;
            var error = failure != null ? failure.message() : publisher.getLastError();
            var errorAt = failure != null ? failure.at() : null;
            var sent = lastSent;
            return new ColorStatus(true, toHex(tracker.getCurrentColor()), properties.getProperty("broker"),
                    properties.getProperty("topic"), publisher.isConnected(),
                    sent == null ? null : sent.payload(), sent == null ? null : sent.at(), error, errorAt,
                    stats.snapshot(idle && idlePeriod > updatePeriod));
        }

        /**
         * Переход между черным, серым и цветным отправляется всегда, даже если он меньше допусков:
         * иначе светильник не погаснет при затемнении экрана. Сюда попадают только цвета,
         * продержавшиеся holdTime мс, см. {@link StabilityGate}. Первый кадр после запуска
         * отправляется всегда, чтобы светильник сразу пришел в состояние экрана.
         */
        private boolean hasChanged(Color color) {
            if (firstFrame) {
                firstFrame = false;
                tracker.setCurrentColor(color);
                return true;
            }
            var zoneChanged = zones.zoneOf(tracker.getCurrentColor()) != zones.zoneOf(color);
            if (tracker.hasColorChanged(color)) {
                return true;
            }
            if (zoneChanged) {
                tracker.setCurrentColor(color);
                return true;
            }
            return false;
        }

        private void processFrame() {
            try {
                var start = System.nanoTime();
                var screenshot = screenShooter.getScreenshot();
                var captured = System.nanoTime();
                var detected = detector.detect(screenshot);
                idle = idleTracker.update(detected, captured);
                var color = zones.normalize(detected);
                captureFailure = null;
                if ((firstFrame || stability.isStable(color)) && hasChanged(color)) {
                    var hsb = zones.toOutput(color, modifier, limit);
                    publisher.publish(hsb);
                    if (publisher.isConnected()) {
                        lastSent = new Sent(MQTTColorPublisher.payload(hsb), Instant.now());
                    }
                }
                var end = System.nanoTime();
                stats.record(captured - start, end - captured, end);
            } catch (RuntimeException e) {
                if (captureFailure == null) {
                    log.error("Ошибка захвата экрана", e);
                }
                captureFailure = new Failure("Ошибка захвата экрана: " + e.getMessage(), Instant.now());
            }
        }
    }
}
