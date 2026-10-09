package ru.mkilord.colortomqttapp.core.publishing;

import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;

public interface ColorPublisherFactory {

    /** Для захвата: подключение в фоне, вызов не ждет брокер. */
    ColorPublisher open(MqttConnection connection);

    /**
     * Для разовой отправки: ждет подключения.
     *
     * @throws ru.mkilord.colortomqttapp.domain.error.UnavailableException если подключиться не удалось
     */
    ColorPublisher openNow(MqttConnection connection);
}
