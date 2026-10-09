package ru.mkilord.colortomqttapp.core.detection;

import ru.mkilord.colortomqttapp.core.sampling.PixelSamples;
import ru.mkilord.colortomqttapp.core.sampling.PointSampler;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Преобладающий цвет: красный объект на темном фоне дает красный, а не грязно-серый.
 * <p>
 * Цветные точки (насыщенность от 20%, яркость от 15%) раскладываются по 24 секторам тона.
 * Точка весит насыщенность x яркость. Берется самый тяжелый сектор вместе с соседними,
 * результат считается взвешенным средним их точек. Если цветных точек в группе меньше
 * заданной доли кадра, цвета на экране нет или он мелкий, и возвращается простое среднее.
 */
public final class DominantColorDetector implements ColorDetector {

    static final int BINS = 24;
    static final float MIN_SATURATION = 0.2f;
    static final float MIN_BRIGHTNESS = 0.15f;

    private final PointSampler sampler;
    private final float minShare;

    public DominantColorDetector(PointSampler sampler, float minSharePercent) {
        this.sampler = sampler;
        this.minShare = minSharePercent / 100f;
    }

    @Override
    public Color detect(BufferedImage image) {
        var samples = PixelSamples.collect(sampler, image);
        var bins = new int[samples.length];
        var weights = new float[samples.length];
        var binWeight = new double[BINS];
        var hsb = new float[3];
        for (int i = 0; i < samples.length; i++) {
            var rgb = samples[i];
            Color.RGBtoHSB(PixelSamples.red(rgb), PixelSamples.green(rgb), PixelSamples.blue(rgb), hsb);
            if (hsb[1] < MIN_SATURATION || hsb[2] < MIN_BRIGHTNESS) {
                bins[i] = -1;
                continue;
            }
            bins[i] = Math.min((int) (hsb[0] * BINS), BINS - 1);
            weights[i] = hsb[1] * hsb[2];
            binWeight[bins[i]] += weights[i];
        }

        var best = heaviestGroup(binWeight);
        if (best < 0) {
            return AverageColorDetector.average(samples);
        }

        double r = 0, g = 0, b = 0, total = 0;
        var count = 0;
        for (int i = 0; i < samples.length; i++) {
            if (bins[i] < 0 || circularDistance(bins[i], best) > 1) {
                continue;
            }
            var rgb = samples[i];
            r += PixelSamples.red(rgb) * weights[i];
            g += PixelSamples.green(rgb) * weights[i];
            b += PixelSamples.blue(rgb) * weights[i];
            total += weights[i];
            count++;
        }
        if (count < minShare * samples.length || total <= 0) {
            return AverageColorDetector.average(samples);
        }
        return new Color((int) (r / total), (int) (g / total), (int) (b / total));
    }

    /** Сектор, который вместе с соседями весит больше всех, или -1, если цветных точек нет. */
    private static int heaviestGroup(double[] binWeight) {
        var best = -1;
        var bestWeight = 0.0;
        for (int bin = 0; bin < BINS; bin++) {
            var group = binWeight[bin] + binWeight[(bin + 1) % BINS] + binWeight[(bin + BINS - 1) % BINS];
            if (group > bestWeight) {
                bestWeight = group;
                best = bin;
            }
        }
        return best;
    }

    private static int circularDistance(int a, int b) {
        var d = Math.abs(a - b);
        return Math.min(d, BINS - d);
    }
}
