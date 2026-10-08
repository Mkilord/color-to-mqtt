package ru.mkilord.colortomqttapp.core.publisher;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.core.HSBColor;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class MQTTColorPublisherTest {

    @Test
    void messageIsJsonWithRoundedValuesRegardlessOfLocale() {
        var previous = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            var message = MQTTColorPublisher.createMessage(new HSBColor(210.4f, 64.6f, 48f));

            assertThat(new String(message.getPayload())).isEqualTo("{\"hue\":210,\"sat\":65,\"brightness\":48}");
            assertThat(message.getQos()).isZero();
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void kelvinIsAddedWhenSet() {
        assertThat(MQTTColorPublisher.payload(new HSBColor(0, 0, 20), 5000))
                .isEqualTo("{\"hue\":0,\"sat\":0,\"brightness\":20,\"kelvin\":5000}");
        assertThat(MQTTColorPublisher.payload(new HSBColor(0, 0, 20), null))
                .isEqualTo("{\"hue\":0,\"sat\":0,\"brightness\":20}");
    }
}
