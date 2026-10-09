package ru.mkilord.colortomqttapp.application;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionServiceTest {

    private final InMemorySettingsStore store = new InMemorySettingsStore();
    private final List<Object> events = new ArrayList<>();
    private final ConnectionService service = new ConnectionService(store, events::add);

    @Test
    void emptyPasswordKeepsSavedOne() {
        store.connection = new MqttConnection("tcp://h:1883", "t", "user", "secret");

        var updated = service.update(new MqttConnection("tcp://h:1883", "t2", "user", ""));

        assertThat(updated.password()).isEqualTo("secret");
        assertThat(updated.topic()).isEqualTo("t2");
        assertThat(events).hasSize(1);
    }

    @Test
    void clearingUsernameDropsPassword() {
        store.connection = new MqttConnection("tcp://h:1883", "t", "user", "secret");

        var updated = service.update(new MqttConnection("tcp://h:1883", "t", "", ""));

        assertThat(updated.hasCredentials()).isFalse();
        assertThat(store.connection.password()).isEmpty();
    }

    @Test
    void unchangedConnectionIsNotAnnounced() {
        service.update(store.connection);

        assertThat(events).isEmpty();
    }
}
