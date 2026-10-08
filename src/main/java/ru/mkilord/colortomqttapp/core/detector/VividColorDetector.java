package ru.mkilord.colortomqttapp.core.detector;

import ru.mkilord.colortomqttapp.core.processor.Processor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Properties;

/**
 * Средний цвет, в котором каждый пиксель весит тем больше, чем он насыщеннее и ярче.
 * Темный фон и серые элементы почти не разбавляют цвет, но несколько цветов смешиваются.
 */
public final class VividColorDetector implements ColorDetector {

    private final Processor processor;

    public VividColorDetector(Properties properties) {
        this(Samples.processor(properties));
    }

    VividColorDetector(Processor processor) {
        this.processor = processor;
    }

    @Override
    public Color detect(BufferedImage image) {
        var samples = Samples.collect(processor, image);
        var hsb = new float[3];
        double r = 0, g = 0, b = 0, total = 0;
        for (var rgb : samples) {
            Color.RGBtoHSB(Samples.red(rgb), Samples.green(rgb), Samples.blue(rgb), hsb);
            var weight = hsb[1] * hsb[2];
            r += Samples.red(rgb) * weight;
            g += Samples.green(rgb) * weight;
            b += Samples.blue(rgb) * weight;
            total += weight;
        }
        // Кадр без цвета: веса почти нулевые, берем обычное среднее.
        if (total < 0.01 * samples.length) {
            return AverageColorDetector.average(samples);
        }
        return new Color((int) (r / total), (int) (g / total), (int) (b / total));
    }
}
