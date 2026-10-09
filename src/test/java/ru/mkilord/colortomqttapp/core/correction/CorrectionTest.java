package ru.mkilord.colortomqttapp.core.correction;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.domain.color.HsbColor;
import ru.mkilord.colortomqttapp.domain.settings.ColorRange;
import ru.mkilord.colortomqttapp.domain.settings.ColorRanges;
import ru.mkilord.colortomqttapp.domain.settings.CorrectionSettings;
import ru.mkilord.colortomqttapp.domain.settings.HueShifts;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CorrectionTest {

    private static final float[] GREEN_ONLY = {0, 0, -20, 0, 0, 0};

    @Test
    void boostMovesSaturationTowardsFull() {
        assertThat(ColorCorrector.boost(65, 40)).isCloseTo(79, within(0.01f));
        assertThat(ColorCorrector.boost(100, 40)).isEqualTo(100);
        assertThat(ColorCorrector.boost(50, 0)).isEqualTo(50);
    }

    @Test
    void hueShiftForGreenDoesNotTouchRedAndBlue() {
        assertThat(ColorCorrector.mapHue(0, GREEN_ONLY)).isEqualTo(0);
        assertThat(ColorCorrector.mapHue(240, GREEN_ONLY)).isEqualTo(240);
        assertThat(ColorCorrector.mapHue(120, GREEN_ONLY)).isCloseTo(100, within(0.01f));
        assertThat(ColorCorrector.mapHue(90, GREEN_ONLY)).isCloseTo(80, within(0.01f));
    }

    @Test
    void rotateWrapsAround() {
        assertThat(ColorCorrector.rotate(350, 20)).isCloseTo(10, within(0.01f));
        assertThat(ColorCorrector.rotate(10, -20)).isCloseTo(350, within(0.01f));
    }

    @Test
    void rangesLimitResult() {
        var settings = CorrectionSettings.builder()
                .saturationBoost(0)
                .ranges(new ColorRanges(new ColorRange(0, 360), new ColorRange(0, 100), new ColorRange(10, 60)))
                .build();
        var result = new ColorCorrector(settings).apply(new HsbColor(200, 50, 90));

        assertThat(result.brightness()).isEqualTo(60);
        assertThat(result.saturation()).isEqualTo(50);
    }

    @Test
    void converterTurnsBlackOffAndKeepsGraySaturationAboveZero() {
        var settings = CorrectionSettings.builder().hueShifts(HueShifts.NONE).build();
        var converter = new LampColorConverter(new ColorZones(5, 12), new ColorCorrector(settings));

        assertThat(converter.convert(new Color(5, 5, 5))).isEqualTo(HsbColor.BLACK);
        var gray = converter.convert(new Color(128, 128, 128));
        assertThat(gray.saturation()).isEqualTo(LampColorConverter.GRAY_SATURATION);
        assertThat(gray.brightness()).isCloseTo(50, within(1f));
        assertThat(converter.convert(Color.RED).saturation()).isEqualTo(100);
    }

    @Test
    void zonesNormalizeNoise() {
        var zones = new ColorZones(5, 12);

        assertThat(zones.normalize(new Color(8, 3, 3))).isEqualTo(Color.BLACK);
        assertThat(zones.normalize(new Color(120, 118, 125))).isEqualTo(new Color(125, 125, 125));
        assertThat(zones.zoneOf(Color.BLUE)).isEqualTo(ColorZone.COLOR);
    }
}
