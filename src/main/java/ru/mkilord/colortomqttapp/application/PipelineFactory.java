package ru.mkilord.colortomqttapp.application;

import org.springframework.stereotype.Component;
import ru.mkilord.colortomqttapp.core.capture.ScreenCapture;
import ru.mkilord.colortomqttapp.core.correction.ColorCorrector;
import ru.mkilord.colortomqttapp.core.correction.ColorZones;
import ru.mkilord.colortomqttapp.core.correction.LampColorConverter;
import ru.mkilord.colortomqttapp.core.detection.ColorDetector;
import ru.mkilord.colortomqttapp.core.pipeline.CapturePipeline;
import ru.mkilord.colortomqttapp.core.publishing.ColorPublisherFactory;
import ru.mkilord.colortomqttapp.core.sampling.PointSampler;
import ru.mkilord.colortomqttapp.core.tracking.ColorChangeTracker;
import ru.mkilord.colortomqttapp.core.tracking.StabilityGate;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.error.UnavailableException;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;

/**
 * Собирает цикл захвата из настроек профиля. Внешние зависимости (экран, MQTT) приходят
 * через DI, алгоритмы выбираются по enum в настройках.
 */
@Component
public class PipelineFactory {

    private final ScreenCapture screen;
    private final ColorPublisherFactory publishers;

    public PipelineFactory(ScreenCapture screen, ColorPublisherFactory publishers) {
        this.screen = screen;
        this.publishers = publishers;
    }

    public CapturePipeline create(ProfileSettings settings, MqttConnection connection) {
        var screenSize = screen.screenSize();
        if (screenSize == null) {
            throw new UnavailableException("Захват экрана работает только на рабочем столе с графической оболочкой");
        }
        var capture = settings.capture();
        var zones = ColorZones.of(settings.detection());
        var sampler = PointSampler.of(capture.sampling(), capture.cellSize());
        var sending = settings.sending();
        return CapturePipeline.builder()
                .screen(screen, ScreenCapture.centered(screenSize, capture.width(), capture.height()))
                .detector(ColorDetector.of(settings.detection(), sampler))
                .zones(zones)
                .tracker(ColorChangeTracker.of(sending))
                .stability(new StabilityGate(ColorChangeTracker.of(sending), zones, sending.holdTimeMs(), System::nanoTime))
                .converter(new LampColorConverter(zones, new ColorCorrector(settings.correction())))
                .publisher(publishers.open(connection))
                .periods(capture.framePeriodMs(), capture.idlePeriodMs())
                .build();
    }
}
