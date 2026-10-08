package ru.mkilord.colortomqttapp.core.zone;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.HSBColor;
import ru.mkilord.colortomqttapp.core.limit.ColorLimit;
import ru.mkilord.colortomqttapp.core.modifier.ColorModifier;

import java.awt.Color;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ColorZonesTest {

    private final ColorZones zones = new ColorZones(5, 12);
    // Сдвиг +10 по насыщенности и +2 по яркости, как в старых настройках по умолчанию.
    private final ColorModifier modifier = c -> new HSBColor(c.getHue(), Math.min(c.getSaturation() + 10, 100),
            Math.min(c.getBrightness() + 2, 100));
    // Минимум насыщенности 10.
    private final ColorLimit limit = c -> new HSBColor(c.getHue(), Math.max(c.getSaturation(), 10), c.getBrightness());

    @Test
    void classifiesBlackGrayAndColor() {
        assertThat(zones.zoneOf(Color.BLACK)).isEqualTo(ColorZone.BLACK);
        assertThat(zones.zoneOf(new Color(10, 9, 9))).isEqualTo(ColorZone.BLACK);
        assertThat(zones.zoneOf(new Color(30, 28, 28))).isEqualTo(ColorZone.GRAY);
        assertThat(zones.zoneOf(new Color(240, 240, 250))).isEqualTo(ColorZone.GRAY);
        assertThat(zones.zoneOf(new Color(200, 40, 40))).isEqualTo(ColorZone.COLOR);
    }

    @Test
    void blackIsSentAsZeroWithoutCorrectionAndLimits() {
        var out = zones.toOutput(zones.normalize(new Color(3, 2, 2)), modifier, limit);

        assertThat(out).isEqualTo(new HSBColor(0, 0, 0));
    }

    @Test
    void grayIsSentWithoutHueAndSaturation() {
        var out = zones.toOutput(zones.normalize(new Color(51, 48, 48)), modifier, limit);

        assertThat(out.getHue()).isZero();
        assertThat(out.getSaturation()).isZero();
        assertThat(out.getBrightness()).isCloseTo(22f, within(0.01f));
    }

    @Test
    void noisyGraysNormalizeToSameColor() {
        assertThat(zones.normalize(new Color(30, 28, 28))).isEqualTo(zones.normalize(new Color(28, 30, 28)));
    }

    @Test
    void colorIsCorrectedAsUsual() {
        var out = zones.toOutput(new Color(200, 40, 40), modifier, limit);

        assertThat(out).isEqualTo(limit.applyFor(modifier.modify(new HSBColor(new Color(200, 40, 40)))));
    }

    @Test
    void zeroThresholdsDisableZones() {
        var disabled = new ColorZones(0, 0);

        assertThat(disabled.zoneOf(Color.BLACK)).isEqualTo(ColorZone.COLOR);
        assertThat(disabled.normalize(new Color(30, 28, 28))).isEqualTo(new Color(30, 28, 28));
    }

    @Test
    void missingPropertiesUseDefaults() {
        var fromEmpty = new ColorZones(new Properties());

        assertThat(fromEmpty.zoneOf(new Color(10, 10, 10))).isEqualTo(ColorZone.BLACK);
        assertThat(fromEmpty.zoneOf(new Color(100, 95, 95))).isEqualTo(ColorZone.GRAY);
    }
}
