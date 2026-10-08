package ru.mkilord.colortomqttapp.core.detector;

import lombok.experimental.FieldDefaults;
import ru.mkilord.colortomqttapp.core.processor.Processor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class AverageColorDetector implements ColorDetector {
    Processor processor;

    public AverageColorDetector(Properties properties) {
        this(Samples.processor(properties));
    }

    AverageColorDetector(Processor processor) {
        this.processor = processor;
    }

    /**
     * Обычное среднее по точкам в формате 0xRRGGBB.
     */
    static Color average(int[] samples) {
        long r = 0, g = 0, b = 0;
        for (var rgb : samples) {
            r += Samples.red(rgb);
            g += Samples.green(rgb);
            b += Samples.blue(rgb);
        }
        return new Color((int) (r / samples.length), (int) (g / samples.length), (int) (b / samples.length));
    }

    public Color detect(BufferedImage image) {
        return average(Samples.collect(processor, image));
    }
}