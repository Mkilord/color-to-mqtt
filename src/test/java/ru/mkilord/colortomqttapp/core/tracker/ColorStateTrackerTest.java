package ru.mkilord.colortomqttapp.core.tracker;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.TestProperties;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

class ColorStateTrackerTest {

    @Test
    void simpleTrackerRemembersNewColor() {
        var tracker = new SimpleColorStateTracker();

        assertThat(tracker.hasColorChanged(Color.RED)).isTrue();
        assertThat(tracker.getCurrentColor()).isEqualTo(Color.RED);
        assertThat(tracker.hasColorChanged(Color.RED)).isFalse();
    }

    @Test
    void defaultTrackerIgnoresSmallChanges() {
        var tracker = new DefaultColorStateTracker(TestProperties.defaults());

        assertThat(tracker.hasColorChanged(new Color(10, 10, 10))).isFalse();
        assertThat(tracker.getCurrentColor()).isEqualTo(Color.BLACK);
        assertThat(tracker.hasColorChanged(Color.WHITE)).isTrue();
        assertThat(tracker.getCurrentColor()).isEqualTo(Color.WHITE);
    }

    @Test
    void toleranceTrackerTreatsHueAsCircle() {
        var tracker = new ToleranceColorStateTracker(TestProperties.defaults());
        var almostRed = Color.getHSBColor(350f / 360, 1f, 1f);
        var red = Color.getHSBColor(5f / 360, 1f, 1f);

        assertThat(tracker.hasColorChanged(almostRed)).isTrue();
        assertThat(tracker.hasColorChanged(red)).isFalse();
        assertThat(tracker.hasColorChanged(Color.GREEN)).isTrue();
        assertThat(tracker.getCurrentColor()).isEqualTo(Color.GREEN);
    }
}
