package ru.mkilord.colortomqttapp.core.modifier;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.TestProperties;
import ru.mkilord.colortomqttapp.core.HSBColor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DefaultColorModifierTest {

    @Test
    void shiftsHueAroundCircleAndClampsOthers() {
        var props = TestProperties.defaults();
        props.setProperty("modifyHue", "20");
        props.setProperty("modifySaturation", "10");
        props.setProperty("modifyBrightness", "-30");

        var result = new DefaultColorModifier(props).modify(new HSBColor(350, 95, 20));

        assertThat(result.getHue()).isCloseTo(10, within(0.001f));
        assertThat(result.getSaturation()).isEqualTo(100);
        assertThat(result.getBrightness()).isEqualTo(0);
    }

    @Test
    void negativeHueShiftWrapsBelowZero() {
        var props = TestProperties.defaults();
        props.setProperty("modifyHue", "-30");

        var result = new DefaultColorModifier(props).modify(new HSBColor(10, 50, 50));

        assertThat(result.getHue()).isCloseTo(340, within(0.001f));
    }

    @Test
    void saturationBoostMovesSaturationTowardsFull() {
        var props = TestProperties.defaults();
        props.setProperty("modifyHue", "0");
        props.setProperty("modifySaturation", "0");
        props.setProperty("modifyBrightness", "0");
        props.setProperty("saturationBoost", "40");

        var result = new DefaultColorModifier(props).modify(new HSBColor(358, 65, 20));

        assertThat(result.getSaturation()).isCloseTo(79, within(0.001f));
        assertThat(result.getBrightness()).isEqualTo(20);
    }

    @Test
    void missingBoostMeansNoChange() {
        var props = TestProperties.defaults();
        props.remove("saturationBoost");
        props.setProperty("modifySaturation", "0");

        assertThat(new DefaultColorModifier(props).modify(new HSBColor(10, 65, 50)).getSaturation()).isEqualTo(65);
    }
}
