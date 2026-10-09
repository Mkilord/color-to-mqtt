package ru.mkilord.colortomqttapp.core.detection;

import ru.mkilord.colortomqttapp.core.sampling.PointSampler;
import ru.mkilord.colortomqttapp.domain.settings.DetectionSettings;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Сводит кадр к одному цвету.
 */
@FunctionalInterface
public interface ColorDetector {

    Color detect(BufferedImage image);

    static ColorDetector of(DetectionSettings settings, PointSampler sampler) {
        return switch (settings.method()) {
            case DOMINANT -> new DominantColorDetector(sampler, settings.minSharePercent());
            case VIVID -> new VividColorDetector(sampler);
            case AVERAGE -> new AverageColorDetector(sampler);
        };
    }
}
