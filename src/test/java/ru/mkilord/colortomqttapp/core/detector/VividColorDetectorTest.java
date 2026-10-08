package ru.mkilord.colortomqttapp.core.detector;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.processor.GridProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class VividColorDetectorTest {

    @Test
    void brightColorsOutweighDarkBackground() {
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                var red = x >= 30 && x < 70 && y >= 30 && y < 70;
                image.setRGB(x, y, (red ? new Color(200, 20, 30) : new Color(15, 15, 20)).getRGB());
            }
        }

        var vivid = new VividColorDetector(new GridProcessor(2)).detect(image);

        assertThat(vivid.getRed()).isGreaterThan(150);
    }

    @Test
    void grayFrameGivesPlainAverage() {
        var image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                image.setRGB(x, y, new Color(90, 90, 90).getRGB());
            }
        }

        assertThat(new VividColorDetector(new GridProcessor(1)).detect(image)).isEqualTo(new Color(90, 90, 90));
    }
}
