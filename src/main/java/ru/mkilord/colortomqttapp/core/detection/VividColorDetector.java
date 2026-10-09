package ru.mkilord.colortomqttapp.core.detection;

import ru.mkilord.colortomqttapp.core.sampling.PixelSamples;
import ru.mkilord.colortomqttapp.core.sampling.PointSampler;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Среднее, где точка весит насыщенность x яркость: темный фон и серое почти не разбавляют цвет,
 * но несколько цветов смешиваются.
 */
public final class VividColorDetector implements ColorDetector {

    private final PointSampler sampler;

    public VividColorDetector(PointSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public Color detect(BufferedImage image) {
        var samples = PixelSamples.collect(sampler, image);
        var hsb = new float[3];
        double r = 0, g = 0, b = 0, total = 0;
        for (var rgb : samples) {
            Color.RGBtoHSB(PixelSamples.red(rgb), PixelSamples.green(rgb), PixelSamples.blue(rgb), hsb);
            var weight = hsb[1] * hsb[2];
            r += PixelSamples.red(rgb) * weight;
            g += PixelSamples.green(rgb) * weight;
            b += PixelSamples.blue(rgb) * weight;
            total += weight;
        }
        if (total < 0.01 * samples.length) {
            return AverageColorDetector.average(samples);
        }
        return new Color((int) (r / total), (int) (g / total), (int) (b / total));
    }
}
