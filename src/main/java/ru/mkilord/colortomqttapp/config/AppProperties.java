package ru.mkilord.colortomqttapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;

import java.nio.file.Path;

/**
 * @param storageDir папка с settings.yaml и profiles, относительно рабочей директории
 * @param mqtt       подключение по умолчанию, пока в интерфейсе ничего не сохранено
 */
@ConfigurationProperties("app")
public record AppProperties(Path storageDir, MqttConnection mqtt) {

    public AppProperties {
        storageDir = (storageDir == null ? Path.of(".") : storageDir).toAbsolutePath().normalize();
    }
}
