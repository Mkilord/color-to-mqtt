package ru.mkilord.colortomqttapp.core.detector;

import ru.mkilord.colortomqttapp.core.AbstractFactory;
import ru.mkilord.colortomqttapp.core.processor.Processor;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;
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
        var reader = reader(image);
        var buffer = new int[][]{new int[1024]};
        var count = new int[1];
        processor.process(image.getWidth(), image.getHeight(), (x, y) -> {
            if (count[0] == buffer[0].length) {
                buffer[0] = Arrays.copyOf(buffer[0], count[0] * 2);
            }
            buffer[0][count[0]++] = reader.rgb(x, y);
        });
        if (count[0] == 0) {
            return new int[]{reader.rgb(image.getWidth() / 2, image.getHeight() / 2)};
        }
        return Arrays.copyOf(buffer[0], count[0]);
    }

    @FunctionalInterface
    interface PixelReader {
        int rgb(int x, int y);
    }

    /**
     * Снимок экрана приходит как TYPE_INT_RGB: пиксели читаются прямо из массива, без
     * преобразования через ColorModel на каждый вызов getRGB. Для других форматов getRGB.
     */
    static PixelReader reader(BufferedImage image) {
        var raster = image.getRaster();
        var type = image.getType();
        if ((type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB)
                && raster.getDataBuffer() instanceof DataBufferInt buffer
                && raster.getSampleModel() instanceof SinglePixelPackedSampleModel model
                && raster.getSampleModelTranslateX() == 0 && raster.getSampleModelTranslateY() == 0) {
            var data = buffer.getData();
            var offset = buffer.getOffset();
            var stride = model.getScanlineStride();
            return (x, y) -> data[offset + y * stride + x] & 0xFFFFFF;
        }
        return (x, y) -> image.getRGB(x, y) & 0xFFFFFF;
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
