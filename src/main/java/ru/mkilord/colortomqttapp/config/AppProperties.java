package ru.mkilord.colortomqttapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;

import java.nio.file.Path;

/**
 * @param storageDir папка с settings.yaml и profiles, относительно рабочей директории
 * @param mqtt       подключение по умолчанию, пока в интерфейсе ничего не сохранено
 */
@ConfigurationProperties("app")
public record AppProperties(String storageDir, MqttConnection mqtt) {

    /**
     * Путь строится здесь, а не конвертером Spring: тот разрешает относительный путь
     * через загрузчик ресурсов веб-приложения, и "." уходит во временную папку Tomcat.
     */
    public Path storagePath() {
        var dir = storageDir == null || storageDir.isBlank() ? "." : storageDir.strip();
        return Path.of(dir).toAbsolutePath().normalize();
    }
}
