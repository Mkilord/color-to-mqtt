package ru.mkilord.colortomqttapp.application;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.mkilord.colortomqttapp.core.capture.ScreenCapture;
import ru.mkilord.colortomqttapp.core.pipeline.CapturePipeline;
import ru.mkilord.colortomqttapp.core.publishing.ColorPayload;
import ru.mkilord.colortomqttapp.core.publishing.ColorPublisherFactory;
import ru.mkilord.colortomqttapp.domain.color.HsbColor;
import ru.mkilord.colortomqttapp.domain.error.UnavailableException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;

/**
 * Запуск и остановка захвата, его состояние, снимок области и разовая отправка цвета.
 */
@Slf4j
@Service
public class CaptureService {

    private final ProfileService profiles;
    private final ConnectionService connections;
    private final PipelineFactory pipelines;
    private final ColorPublisherFactory publishers;
    private final ScreenCapture screen;

    private CapturePipeline pipeline;

    public CaptureService(ProfileService profiles, ConnectionService connections, PipelineFactory pipelines,
                          ColorPublisherFactory publishers, ScreenCapture screen) {
        this.profiles = profiles;
        this.connections = connections;
        this.pipelines = pipelines;
        this.publishers = publishers;
        this.screen = screen;
    }

    public synchronized boolean isRunning() {
        return pipeline != null;
    }

    public synchronized void start() {
        if (pipeline != null) {
            return;
        }
        pipeline = pipelines.create(profiles.activeSettings(), connections.current());
        pipeline.start();
        log.info("Захват цвета запущен, профиль «{}»", profiles.active());
    }

    @PreDestroy
    public synchronized void stop() {
        if (pipeline == null) {
            return;
        }
        pipeline.stop();
        pipeline = null;
        log.info("Захват цвета остановлен");
    }

    @EventListener
    public synchronized void onSettingsChanged(SettingsChangedEvent event) {
        if (pipeline != null) {
            log.info("Перезапуск захвата: {}", event.reason());
            stop();
            start();
        }
    }

    public synchronized CaptureStatus status() {
        var connection = connections.current();
        if (pipeline == null) {
            return new CaptureStatus(false, hex(Color.BLACK), connection.broker(), connection.topic(),
                    null, null, null, null, null, null);
        }
        var state = pipeline.state();
        return new CaptureStatus(true, hex(state.color()), connection.broker(), connection.topic(), state.connected(),
                state.lastPayload(), state.lastSentAt(), state.error(), state.errorAt(), state.performance());
    }

    /**
     * Отправляет цвет как есть. Если захват идет, через его соединение, иначе через новое.
     *
     * @return отправленное сообщение
     */
    public String sendTestColor(HsbColor color) {
        CapturePipeline running;
        synchronized (this) {
            running = pipeline;
        }
        if (running != null) {
            if (!running.publisher().publish(color)) {
                throw new UnavailableException("Нет соединения с брокером");
            }
            return ColorPayload.of(color);
        }
        try (var publisher = publishers.openNow(connections.current())) {
            if (!publisher.publish(color)) {
                throw new UnavailableException("Сообщение не отправлено: " + publisher.lastError());
            }
            return ColorPayload.of(color);
        }
    }

    /** Снимок области заданного размера в центре экрана, JPEG. */
    public byte[] snapshot(int width, int height) {
        var size = screen.screenSize();
        if (size == null) {
            throw new UnavailableException("Захват экрана работает только на рабочем столе с графической оболочкой");
        }
        var image = screen.capture(ScreenCapture.centered(size, width, height));
        try (var out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось закодировать снимок", e);
        }
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
