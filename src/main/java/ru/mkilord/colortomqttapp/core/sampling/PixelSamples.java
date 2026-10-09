package ru.mkilord.colortomqttapp.core.sampling;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Arrays;

/**
 * Пиксели кадра в точках обхода, в формате 0xRRGGBB.
 */
public final class PixelSamples {

    private PixelSamples() {
    }

    /**
     * Если точек нет (область меньше клетки), берется центральный пиксель.
     */
    public static int[] collect(PointSampler sampler, BufferedImage image) {
        var reader = reader(image);
        var buffer = new int[][]{new int[1024]};
        var count = new int[1];
        sampler.forEach(image.getWidth(), image.getHeight(), (x, y) -> {
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
     * Снимок экрана приходит как TYPE_INT_RGB: пиксели читаются прямо из массива,
     * без преобразования через ColorModel на каждый вызов getRGB.
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

    public static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    public static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    public static int blue(int rgb) {
        return rgb & 0xFF;
    }
}
