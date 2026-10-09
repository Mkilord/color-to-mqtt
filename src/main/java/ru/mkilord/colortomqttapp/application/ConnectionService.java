package ru.mkilord.colortomqttapp.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;

/**
 * Подключение к брокеру, общее для всех профилей.
 */
@Service
public class ConnectionService {

    private final SettingsStore store;
    private final ApplicationEventPublisher events;

    public ConnectionService(SettingsStore store, ApplicationEventPublisher events) {
        this.store = store;
        this.events = events;
    }

    public MqttConnection current() {
        return store.loadConnection();
    }

    /**
     * Пустое поле пароля оставляет сохраненный пароль, очистка логина удаляет и пароль.
     */
    public MqttConnection update(MqttConnection edited) {
        MqttConnection updated;
        boolean changed;
        synchronized (this) {
            var current = current();
            updated = current.merge(edited);
            changed = !updated.equals(current);
            if (changed) {
                store.saveConnection(updated);
            }
        }
        if (changed) {
            events.publishEvent(new SettingsChangedEvent("изменено подключение"));
        }
        return updated;
    }
}
