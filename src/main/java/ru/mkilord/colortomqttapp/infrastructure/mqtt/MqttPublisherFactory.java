package ru.mkilord.colortomqttapp.infrastructure.mqtt;

import org.springframework.stereotype.Component;
import ru.mkilord.colortomqttapp.core.publishing.ColorPublisher;
import ru.mkilord.colortomqttapp.core.publishing.ColorPublisherFactory;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.error.UnavailableException;

@Component
public class MqttPublisherFactory implements ColorPublisherFactory {

    @Override
    public ColorPublisher open(MqttConnection connection) {
        var publisher = new MqttColorPublisher(connection);
        publisher.connectInBackground();
        return publisher;
    }

    /** Ждет подключения до 5 секунд. */
    @Override
    public ColorPublisher openNow(MqttConnection connection) {
        var publisher = new MqttColorPublisher(connection);
        if (!publisher.connectNow()) {
            var error = publisher.lastError();
            publisher.close();
            throw new UnavailableException(error);
        }
        return publisher;
    }
}
