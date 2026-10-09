package ru.mkilord.colortomqttapp.core;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.detection.ColorDetector;
import ru.mkilord.colortomqttapp.core.sampling.PointSampler;
import ru.mkilord.colortomqttapp.domain.settings.DetectionMethod;
import ru.mkilord.colortomqttapp.domain.settings.DetectionSettings;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class DetectionTest {

    private static final Color DARK = new Color(15, 15, 20);
    private static final Color RED = new Color(200, 20, 30);
    private static final Color BLUE = new Color(30, 60, 210);

    private static ColorDetector detector(DetectionMethod method) {
        return ColorDetector.of(DetectionSettings.builder().method(method).build(), PointSampler.grid(2));
    }

    private static boolean close(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) <= 2
                && Math.abs(a.getGreen() - b.getGreen()) <= 2
                && Math.abs(a.getBlue() - b.getBlue()) <= 2;
    }

    @Test
    void averageMixesEverything() {
        var half = Frames.square(100, 100, Color.BLACK, Color.WHITE, 0, 0);
        assertThat(detector(DetectionMethod.AVERAGE).detect(Frames.filled(50, 50, Color.ORANGE))).isEqualTo(Color.ORANGE);
        assertThat(detector(DetectionMethod.AVERAGE).detect(half)).isEqualTo(Color.BLACK);
    }

    @Test
    void dominantKeepsColorOnDarkBackground() {
        var frame = Frames.square(100, 100, DARK, RED, 30, 70);

        assertThat(close(detector(DetectionMethod.DOMINANT).detect(frame), RED)).isTrue();
        assertThat(detector(DetectionMethod.AVERAGE).detect(frame).getRed()).isLessThan(100);
    }

    @Test
    void dominantPrefersLargerColor() {
        assertThat(close(detector(DetectionMethod.DOMINANT).detect(Frames.square(100, 100, BLUE, RED, 40, 60)), BLUE)).isTrue();
    }

    @Test
    void dominantFallsBackToAverageForTinyColor() {
        var frame = Frames.square(100, 100, DARK, RED, 48, 52);

        assertThat(detector(DetectionMethod.DOMINANT).detect(frame)).isEqualTo(detector(DetectionMethod.AVERAGE).detect(frame));
    }

    @Test
    void dominantMergesNeighbourHuesAcrossZero() {
        var frame = Frames.square(100, 100, new Color(220, 20, 40), new Color(220, 40, 20), 0, 100);
        var result = detector(DetectionMethod.DOMINANT).detect(frame);

        assertThat(Color.RGBtoHSB(result.getRed(), result.getGreen(), result.getBlue(), null)[1]).isGreaterThan(0.8f);
    }

    @Test
    void vividOutweighsDarkBackground() {
        assertThat(detector(DetectionMethod.VIVID).detect(Frames.square(100, 100, DARK, RED, 30, 70)).getRed()).isGreaterThan(150);
        assertThat(detector(DetectionMethod.VIVID).detect(Frames.filled(10, 10, new Color(90, 90, 90)))).isEqualTo(new Color(90, 90, 90));
    }
}
