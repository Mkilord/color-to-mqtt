package ru.mkilord.colortomqttapp.service.impl;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ru.mkilord.colortomqttapp.core.AbstractFactory;
import ru.mkilord.colortomqttapp.core.HSBColor;
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
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.awt.Color;
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

    @Override
    public synchronized boolean isStart() {
        return pipeline != null;
    }

    @Override
    public synchronized void start() {
        if (pipeline != null) {
            return;
        }
        pipeline = new Pipeline(settingsService.loadOrElseLoadDefault());
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

    private static final class Pipeline {
        private final ScreenShooter screenShooter;
        private final ColorDetector detector;
        private final ColorStateTracker tracker;
        private final ColorModifier modifier;
        private final ColorLimit limit;
        private final ColorPublisher publisher;
        private final RepeatServiceImpl repeater;

        Pipeline(Properties properties) {
            this.screenShooter = new DefaultScreenShooter(properties);
            this.detector = new AbstractFactory<ColorDetector>().get(ColorDetector.DETECTOR_KEY, properties);
            this.tracker = new AbstractFactory<ColorStateTracker>().get(ColorStateTracker.STATE_TRACKER_KEY, properties);
            this.modifier = new DefaultColorModifier(properties);
            this.limit = new DefaultColorLimit(properties);
            this.publisher = new MQTTColorPublisher(properties);
            this.repeater = new RepeatServiceImpl(properties);
        }

        void start() {
            repeater.repeat(this::processFrame);
        }

        void stop() {
            repeater.stop();
            publisher.close();
        }

        private void processFrame() {
            var color = detector.detect(screenShooter.getScreenshot());
            if (tracker.hasColorChanged(color)) {
                var hsb = limit.applyFor(modifier.modify(new HSBColor(color)));
                publisher.publish(hsb);
            }
        }
    }
}
