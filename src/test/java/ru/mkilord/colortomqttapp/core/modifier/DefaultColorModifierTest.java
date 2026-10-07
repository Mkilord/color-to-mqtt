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
}
