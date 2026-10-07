package ru.mkilord.colortomqttapp.core.detector;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.TestProperties;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class AverageColorDetectorTest {

    private final AverageColorDetector detector = new AverageColorDetector(TestProperties.defaults());

    @Test
    void averagesUniformImage() {
        assertThat(detector.detect(filled(200, 200, Color.ORANGE))).isEqualTo(Color.ORANGE);
    }

    @Test
    void averagesTwoHalves() {
        var image = filled(200, 200, Color.BLACK);
        for (int y = 0; y < 200; y++) {
            for (int x = 100; x < 200; x++) {
                image.setRGB(x, y, Color.WHITE.getRGB());
            }
        }

        var color = detector.detect(image);

        assertThat(color.getRed()).isBetween(120, 135);
        assertThat(color.getRed()).isEqualTo(color.getGreen()).isEqualTo(color.getBlue());
    }

    @Test
    void usesCenterPixelWhenAreaIsSmallerThanCell() {
        assertThat(detector.detect(filled(5, 5, Color.BLUE))).isEqualTo(Color.BLUE);
    }

    private static BufferedImage filled(int width, int height, Color color) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }
}
