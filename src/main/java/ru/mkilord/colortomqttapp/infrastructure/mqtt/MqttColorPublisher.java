package ru.mkilord.colortomqttapp.infrastructure.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import ru.mkilord.colortomqttapp.core.publishing.ColorPayload;
import ru.mkilord.colortomqttapp.core.publishing.ColorPublisher;
import ru.mkilord.colortomqttapp.domain.color.HsbColor;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Публикует цвет в MQTT с QoS 0. Подключается в фоне и повторяет попытку каждые 5 секунд,
 * пока не получится: Paho сам переподключается только после первого успешного подключения.
 * Пока связи нет, сообщения не уходят и {@link #publish} возвращает false.
 */
@Slf4j
public final class MqttColorPublisher implements ColorPublisher {

    static final long RETRY_SECONDS = 5;

    private final MqttConnection connection;
    private final MqttClient client;
    private final ScheduledExecutorService connector = Executors.newSingleThreadScheduledExecutor(runnable -> {
        var thread = new Thread(runnable, "mqtt-connect");
        thread.setDaemon(true);
        return thread;
    });
    private volatile String lastError;

    MqttColorPublisher(MqttConnection connection) {
        this.connection = connection;
        try {
            this.client = new MqttClient(connection.broker(), MqttClient.generateClientId(), new MemoryPersistence());
        } catch (MqttException e) {
            throw new IllegalArgumentException("Некорректный адрес MQTT-брокера: " + connection.broker(), e);
        }
        client.setCallback(new ConnectionTracker());
        lastError = "Подключение к " + connection.broker() + "...";
    }

    void connectInBackground() {
        connector.execute(this::connect);
    }

    /** Подключается в текущем потоке, для разовой отправки. */
    boolean connectNow() {
        connect();
        return client.isConnected();
    }

    @Override
    public boolean publish(HsbColor color) {
        if (!client.isConnected()) {
            return false;
        }
        try {
            var message = new MqttMessage(ColorPayload.of(color).getBytes(StandardCharsets.UTF_8));
            message.setQos(0);
            client.publish(connection.topic(), message);
            return true;
        } catch (MqttException e) {
            lastError = "Не удалось отправить цвет: " + describe(e);
            log.warn(lastError);
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public String lastError() {
        return lastError;
    }

    @Override
    public void close() {
        connector.shutdownNow();
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException e) {
            log.warn("Ошибка при закрытии соединения с брокером: {}", e.getMessage());
        }
    }

    private void connect() {
        if (client.isConnected()) {
            return;
        }
        try {
            client.connect(options());
            lastError = null;
            log.info("Подключено к брокеру {}", connection.broker());
        } catch (MqttException e) {
            lastError = "Не удалось подключиться к " + connection.broker() + ": " + describe(e);
            log.warn(lastError);
            if (!connector.isShutdown()) {
                connector.schedule(this::connect, RETRY_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    private MqttConnectOptions options() {
        var options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(5);
        if (connection.hasCredentials()) {
            options.setUserName(connection.username());
            options.setPassword(connection.password().toCharArray());
        }
        return options;
    }

    /** Сообщение Paho плюс причина: «Not authorized» понятнее кода ошибки. */
    static String describe(MqttException e) {
        var message = e.getMessage();
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            message += " (" + e.getCause().getMessage() + ")";
        }
        return message;
    }

    private final class ConnectionTracker implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            lastError = null;
            if (reconnect) {
                log.info("Соединение с брокером {} восстановлено", serverURI);
            }
        }

        @Override
        public void connectionLost(Throwable cause) {
            lastError = "Соединение с " + connection.broker() + " потеряно: " + cause.getMessage() + ". Переподключаюсь";
            log.warn(lastError);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
}
