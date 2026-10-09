package ru.mkilord.colortomqttapp.core.publishing;

import ru.mkilord.colortomqttapp.domain.color.HsbColor;

/**
 * Куда уходит цвет для ламп.
 */
public interface ColorPublisher extends AutoCloseable {

    /**
     * @return true, если сообщение ушло; при обрыве связи false
     */
    boolean publish(HsbColor color);

    boolean isConnected();

    /** Описание последней ошибки соединения или null. */
    String lastError();

    @Override
    void close();
}
