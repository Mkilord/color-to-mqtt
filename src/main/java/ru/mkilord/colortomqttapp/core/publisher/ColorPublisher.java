package ru.mkilord.colortomqttapp.core.publisher;

import ru.mkilord.colortomqttapp.core.HSBColor;

public interface ColorPublisher extends AutoCloseable {
    void publish(HSBColor color);

    /**
     * Освобождает соединение. По умолчанию ничего не делает.
     */
    @Override
    default void close() {
    }
}
