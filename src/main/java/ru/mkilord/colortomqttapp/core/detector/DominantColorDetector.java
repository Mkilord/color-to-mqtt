package ru.mkilord.colortomqttapp.core.detector;

import ru.mkilord.colortomqttapp.core.processor.Processor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Properties;

/**
 * Преобладающий цвет кадра: красный объект на темном фоне дает красный, а не грязно-серый.
 * <p>
 * Цветные пиксели (насыщенность от 20%, яркость от 15%) раскладываются по 24 секторам тона
 * по 15 градусов. Каждый пиксель весит насыщенность x яркость. Выбирается самый тяжелый сектор
 * вместе с соседними, и результат считается как взвешенное среднее их пикселей.
 * Если цветных пикселей в этой группе меньше {@code dominantMinShare} процентов кадра,
 * цвета на экране нет или он мелкий, и возвращается обычный средний цвет.
 */
public final class DominantColorDetector implements ColorDetector {

    public static final String MIN_SHARE_KEY = "dominantMinShare";
    public static final float DEFAULT_MIN_SHARE = 5;
    static final int BINS = 24;
    static final float MIN_SATURATION = 0.2f;
    static final float MIN_BRIGHTNESS = 0.15f;

    private final Processor processor;
    private final float minShare;

    public DominantColorDetector(Properties properties) {
        this(Samples.processor(properties), readShare(properties));
    }

    DominantColorDetector(Processor processor, float minSharePercent) {
        this.processor = processor;
        this.minShare = minSharePercent / 100f;
    }

    private static float readShare(Properties properties) {
        var value = properties.getProperty(MIN_SHARE_KEY);
        return value == null || value.isBlank() ? DEFAULT_MIN_SHARE : Float.parseFloat(value.trim());
    }

    @Override
    public Color detect(BufferedImage image) {
        var samples = Samples.collect(processor, image);
        var bins = new int[samples.length];
        var weights = new float[samples.length];
        var binWeight = new double[BINS];
        var hsb = new float[3];
        for (int i = 0; i < samples.length; i++) {
            var rgb = samples[i];
            Color.RGBtoHSB(Samples.red(rgb), Samples.green(rgb), Samples.blue(rgb), hsb);
            if (hsb[1] < MIN_SATURATION || hsb[2] < MIN_BRIGHTNESS) {
                bins[i] = -1;
                continue;
            }
            bins[i] = Math.min((int) (hsb[0] * BINS), BINS - 1);
            weights[i] = hsb[1] * hsb[2];
            binWeight[bins[i]] += weights[i];
        }

        var best = -1;
        var bestWeight = 0.0;
        for (int bin = 0; bin < BINS; bin++) {
            var group = binWeight[bin] + binWeight[(bin + 1) % BINS] + binWeight[(bin + BINS - 1) % BINS];
            if (group > bestWeight) {
                bestWeight = group;
                best = bin;
            }
        }
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
            r += Samples.red(rgb) * weights[i];
            g += Samples.green(rgb) * weights[i];
            b += Samples.blue(rgb) * weights[i];
            total += weights[i];
            count++;
        }
        if (count < minShare * samples.length || total <= 0) {
            return AverageColorDetector.average(samples);
        }
        return new Color((int) (r / total), (int) (g / total), (int) (b / total));
    }

    private static int circularDistance(int a, int b) {
        var d = Math.abs(a - b);
        return Math.min(d, BINS - d);
    }
}
