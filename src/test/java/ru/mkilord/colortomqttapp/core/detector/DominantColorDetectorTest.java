package ru.mkilord.colortomqttapp.core.detector;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.processor.GridProcessor;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class DominantColorDetectorTest {

    private static final Color DARK = new Color(15, 15, 20);
    private static final Color RED = new Color(200, 20, 30);
    private static final Color BLUE = new Color(30, 60, 210);

    private final DominantColorDetector detector = new DominantColorDetector(new GridProcessor(2), 5);

    /**
     * Кадр 100x100 с фоном и прямоугольником [from, to) по обеим осям.
     */
    private static BufferedImage image(Color background, Color square, int from, int to) {
        var image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                var inside = x >= from && x < to && y >= from && y < to;
                image.setRGB(x, y, (inside ? square : background).getRGB());
            }
        }
        return image;
    }

    private static boolean close(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) <= 2
                && Math.abs(a.getGreen() - b.getGreen()) <= 2
                && Math.abs(a.getBlue() - b.getBlue()) <= 2;
    }

    @Test
    void colorOnDarkBackgroundIsNotMuddied() {
        var frame = image(DARK, RED, 30, 70);

        var dominant = detector.detect(frame);
        var average = new AverageColorDetector(new GridProcessor(2)).detect(frame);

        assertThat(close(dominant, RED)).as("dominant %s", dominant).isTrue();
        assertThat(average.getRed()).isLessThan(100);
    }

    @Test
    void largerColorWins() {
        var frame = image(BLUE, RED, 40, 60);

        assertThat(close(detector.detect(frame), BLUE)).isTrue();
    }

    @Test
    void tinyColorFallsBackToAverage() {
        var frame = image(DARK, RED, 48, 52);

        assertThat(detector.detect(frame)).isEqualTo(new AverageColorDetector(new GridProcessor(2)).detect(frame));
    }

    @Test
    void grayFrameFallsBackToAverage() {
        var gray = new Color(120, 120, 120);

        assertThat(detector.detect(image(gray, gray, 0, 0))).isEqualTo(gray);
    }

    @Test
    void neighbouringHuesAreMergedAcrossZero() {
        var frame = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                // Красный чуть в сторону пурпурного и чуть в сторону оранжевого: тон около 355 и 5 градусов.
                frame.setRGB(x, y, (x < 50 ? new Color(220, 20, 40) : new Color(220, 40, 20)).getRGB());
            }
        }

        var hsb = Color.RGBtoHSB(detector.detect(frame).getRed(), detector.detect(frame).getGreen(),
                detector.detect(frame).getBlue(), null);

        assertThat(hsb[1]).isGreaterThan(0.8f);
    }
}
