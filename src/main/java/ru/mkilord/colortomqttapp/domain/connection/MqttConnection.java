package ru.mkilord.colortomqttapp.domain.connection;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

import java.util.regex.Pattern;

/**
 * Подключение к брокеру, общее для всех профилей.
 *
 * @param username пустой, если брокер без авторизации
 * @param password хранится открытым текстом рядом с приложением
 */
public record MqttConnection(String broker, String topic, String username, String password) {

    private static final Pattern BROKER = Pattern.compile("^(tcp|ssl|ws|wss)://.+");
    private static final int MAX_LENGTH = 255;

    public MqttConnection {
        broker = broker == null ? "" : broker.strip();
        topic = topic == null ? "" : topic.strip();
        username = username == null ? "" : username.strip();
        password = username.isEmpty() || password == null ? "" : password;
        Checks.of("connection")
                .that(BROKER.matcher(broker).matches(), "broker", "Адрес вида tcp://host:1883")
                .that(!topic.isEmpty(), "topic", "Укажите топик")
                .that(topic.indexOf('#') < 0 && topic.indexOf('+') < 0, "topic", "Топик для публикации не может содержать # и +")
                .that(username.length() <= MAX_LENGTH, "username", "Не длиннее 255 символов")
                .that(password.length() <= MAX_LENGTH, "password", "Не длиннее 255 символов")
                .validate();
    }

    public boolean hasCredentials() {
        return !username.isEmpty();
    }

    /**
     * Подключение из формы поверх сохраненного: пустое поле пароля оставляет сохраненный пароль.
     */
    public MqttConnection merge(MqttConnection edited) {
        var keepPassword = edited.password.isEmpty() && edited.hasCredentials();
        return new MqttConnection(edited.broker, edited.topic, edited.username, keepPassword ? password : edited.password);
    }
}
