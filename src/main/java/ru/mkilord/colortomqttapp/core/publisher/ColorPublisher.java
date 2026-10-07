package ru.mkilord.colortomqttapp.core.publisher;

import ru.mkilord.colortomqttapp.core.HSBColor;

public interface ColorPublisher extends AutoCloseable {
    void publish(HSBColor color);

    /**
     * @return true, если сообщения сейчас доходят до получателя
     */
    default boolean isConnected() {
        return true;
    }

    /**
     * @return описание последней ошибки соединения или null
     */
    default String getLastError() {
        return null;
    }

    /**
     * Освобождает соединение. По умолчанию ничего не делает.
     */
    @Override
    default void close() {
    }
}
