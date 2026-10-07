package ru.mkilord.colortomqttapp.core.limit;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.TestProperties;
import ru.mkilord.colortomqttapp.core.HSBColor;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultColorLimitTest {

    @Test
    void clampsToConfiguredRanges() {
        var result = new DefaultColorLimit(TestProperties.defaults()).applyFor(new HSBColor(200, 3, 70));

        assertThat(result.getHue()).isEqualTo(200);
        assertThat(result.getSaturation()).isEqualTo(10);
        assertThat(result.getBrightness()).isEqualTo(70);
    }
}
