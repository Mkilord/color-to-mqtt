package ru.mkilord.colortomqttapp.core.detection;

import ru.mkilord.colortomqttapp.core.sampling.PixelSamples;
import ru.mkilord.colortomqttapp.core.sampling.PointSampler;

import java.awt.Color;
import java.awt.image.BufferedImage;

public final class AverageColorDetector implements ColorDetector {

    private final PointSampler sampler;

    public AverageColorDetector(PointSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public Color detect(BufferedImage image) {
        return average(PixelSamples.collect(sampler, image));
    }

    static Color average(int[] samples) {
        long r = 0, g = 0, b = 0;
        for (var rgb : samples) {
            r += PixelSamples.red(rgb);
            g += PixelSamples.green(rgb);
            b += PixelSamples.blue(rgb);
        }
        return new Color((int) (r / samples.length), (int) (g / samples.length), (int) (b / samples.length));
    }
}
