package ru.mkilord.colortomqttapp.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Log4j2
@Getter
@Configuration
@ConfigurationProperties(prefix = "app")
@FieldDefaults(level = PRIVATE)
public class SettingsConfig {

    /**
     * Файл с настройками, сохраненными со страницы /settings. Путь относительно рабочей директории.
     */
    @Setter
    String settingsFile = "settings.txt";

    /**
     * Папка с профилями настроек, по файлу на профиль. Путь относительно рабочей директории.
     */
    @Setter
    String profilesDir = "profiles";

    @Setter
    Map<String, String> defaultSettings;

    public Path getSettingsFilePath() {
        return Path.of(settingsFile).toAbsolutePath().normalize();
    }

    public Path getProfilesDirPath() {
        return Path.of(profilesDir).toAbsolutePath().normalize();
    }
}
