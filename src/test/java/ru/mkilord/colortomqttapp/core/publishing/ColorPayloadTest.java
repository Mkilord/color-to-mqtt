package ru.mkilord.colortomqttapp.core.publishing;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.domain.color.HsbColor;

import static org.assertj.core.api.Assertions.assertThat;

class ColorPayloadTest {

    @Test
    void valuesAreRoundedWithDotLocale() {
        assertThat(ColorPayload.of(new HsbColor(210.4f, 63.6f, 48)))
                .isEqualTo("{\"hue\":210,\"sat\":64,\"brightness\":48}");
    }
}
