package ru.mkilord.colortomqttapp.core.publisher;

import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import ru.mkilord.colortomqttapp.core.HSBColor;

import java.util.Locale;
import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

/**
 * Публикует цвет в MQTT. Одно соединение на весь сеанс захвата: при обрыве
 * Paho переподключается сам, а сообщения на время обрыва пропускаются.
 */
@Slf4j
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class MQTTColorPublisher implements ColorPublisher {

    String topic;
    String broker;
    MqttClient client;
    @NonFinal
    volatile String lastError;

    public MQTTColorPublisher(Properties properties) {
        this.topic = properties.getProperty("topic");
        this.broker = properties.getProperty("broker");
        try {
            this.client = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());
        } catch (MqttException e) {
            throw new IllegalArgumentException("Некорректный адрес MQTT-брокера: " + broker, e);
        }
        client.setCallback(new ConnectionTracker());
        connect(createOptions(properties.getProperty("username"), properties.getProperty("password")));
    }

    @Override
    public void publish(HSBColor color) {
        publish(color, null);
    }

    @Override
    public void publish(HSBColor color, Integer kelvin) {
        if (!client.isConnected()) {
            log.debug("Нет соединения с брокером, сообщение пропущено");
            return;
        }
        try {
            client.publish(topic, createMessage(color, kelvin));
        } catch (MqttException e) {
            lastError = "Не удалось отправить цвет: " + describe(e);
            log.warn(lastError);
        }
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void close() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException e) {
            log.warn("Ошибка при закрытии соединения с брокером: {}", e.getMessage());
        }
    }

    /**
     * Текст сообщения: JSON с тоном в градусах, насыщенностью и яркостью в процентах, округленными до целых.
     */
    public static String payload(HSBColor color) {
        return payload(color, null);
    }

    /**
     * То же, что {@link #payload(HSBColor)}, плюс поле {@code kelvin}, если температура задана.
     */
    public static String payload(HSBColor color, Integer kelvin) {
        var json = String.format(Locale.ROOT, "{\"hue\":%.0f,\"sat\":%.0f,\"brightness\":%.0f",
                color.getHue(), color.getSaturation(), color.getBrightness());
        return kelvin == null ? json + "}" : json + ",\"kelvin\":" + kelvin + "}";
    }

    static MqttMessage createMessage(HSBColor color) {
        return createMessage(color, null);
    }

    static MqttMessage createMessage(HSBColor color, Integer kelvin) {
        var message = new MqttMessage(payload(color, kelvin).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        message.setQos(0);
        return message;
    }

    private static MqttConnectOptions createOptions(String username, String password) {
        var options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(5);
        if (username != null && !username.isBlank()) {
            options.setUserName(username);
            options.setPassword(password == null ? new char[0] : password.toCharArray());
        }
        return options;
    }

    private void connect(MqttConnectOptions options) {
        try {
            client.connect(options);
            lastError = null;
            log.info("Подключено к брокеру {}", broker);
        } catch (MqttException e) {
            lastError = "Не удалось подключиться к " + broker + ": " + describe(e);
            log.error(lastError);
        }
    }

    /**
     * Сообщение Paho плюс причина: "Not authorized" понятнее, чем код ошибки.
     */
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
            lastError = "Соединение с " + broker + " потеряно: " + cause.getMessage() + ". Переподключаюсь";
            log.warn(lastError);
        }

        @Override
        public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) {
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
}
