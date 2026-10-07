package ru.mkilord.colortomqttapp.core.publisher;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
    MqttClient client;

    public MQTTColorPublisher(Properties properties) {
        this.topic = properties.getProperty("topic");
        var broker = properties.getProperty("broker");
        try {
            this.client = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());
        } catch (MqttException e) {
            throw new IllegalArgumentException("Некорректный адрес MQTT-брокера: " + broker, e);
        }
        connect(createOptions(properties.getProperty("username"), properties.getProperty("password")), broker);
    }

    @Override
    public void publish(HSBColor color) {
        if (!client.isConnected()) {
            log.debug("Нет соединения с брокером, сообщение пропущено");
            return;
        }
        try {
            client.publish(topic, createMessage(color));
        } catch (MqttException e) {
            log.warn("Не удалось отправить цвет: {}", e.getMessage());
        }
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

    static MqttMessage createMessage(HSBColor color) {
        var payload = String.format(Locale.ROOT, "{\"hue\":%.0f,\"sat\":%.0f,\"brightness\":%.0f}",
                color.getHue(), color.getSaturation(), color.getBrightness());
        var message = new MqttMessage(payload.getBytes());
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

    private void connect(MqttConnectOptions options, String broker) {
        try {
            client.connect(options);
            log.info("Подключено к брокеру {}", broker);
        } catch (MqttException e) {
            log.error("Не удалось подключиться к брокеру {}: {}", broker, e.getMessage());
        }
    }
}
