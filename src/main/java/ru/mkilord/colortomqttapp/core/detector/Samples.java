package ru.mkilord.colortomqttapp.core.detector;

import ru.mkilord.colortomqttapp.core.AbstractFactory;
import ru.mkilord.colortomqttapp.core.processor.Processor;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Properties;

/**
 * Пиксели кадра в точках, которые обходит выбранный {@link Processor}.
 */
final class Samples {

    private Samples() {
    }

    static Processor processor(Properties properties) {
        return new AbstractFactory<Processor>().get(Processor.PROCESSOR_KEY, properties);
    }

    /**
     * @return цвета точек в формате 0xRRGGBB; если точек нет (область меньше клетки), центральный пиксель
     */
    static int[] collect(Processor processor, BufferedImage image) {
        var buffer = new int[][]{new int[256]};
        var count = new int[1];
        processor.process(image.getWidth(), image.getHeight(), (x, y) -> {
            if (count[0] == buffer[0].length) {
                buffer[0] = Arrays.copyOf(buffer[0], count[0] * 2);
            }
            buffer[0][count[0]++] = image.getRGB(x, y) & 0xFFFFFF;
        });
        if (count[0] == 0) {
            return new int[]{image.getRGB(image.getWidth() / 2, image.getHeight() / 2) & 0xFFFFFF};
        }
        return Arrays.copyOf(buffer[0], count[0]);
    }

    static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    static int blue(int rgb) {
        return rgb & 0xFF;
    }
}
