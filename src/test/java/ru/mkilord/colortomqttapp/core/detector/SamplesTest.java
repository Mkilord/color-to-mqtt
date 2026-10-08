package ru.mkilord.colortomqttapp.core.detector;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.processor.GridProcessor;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class SamplesTest {

    private static BufferedImage gradient(int type) {
        var image = new BufferedImage(40, 30, type);
        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 40; x++) {
                image.setRGB(x, y, 0xFF000000 | (x * 6) << 16 | (y * 8) << 8 | (x + y));
            }
        }
        return image;
    }

    @Test
    void directReadMatchesGetRgb() {
        var processor = new GridProcessor(3);
        var direct = Samples.collect(processor, gradient(BufferedImage.TYPE_INT_RGB));
        var generic = Samples.collect(processor, gradient(BufferedImage.TYPE_3BYTE_BGR));

        assertThat(direct).hasSize(14 * 10).containsExactly(generic);
    }

    @Test
    void subImageIsReadAtRightOffset() {
        var image = gradient(BufferedImage.TYPE_INT_RGB);
        var sub = image.getSubimage(5, 7, 10, 10);

        assertThat(Samples.collect(new GridProcessor(100), sub)[0]).isEqualTo(image.getRGB(5, 7) & 0xFFFFFF);
    }
}
