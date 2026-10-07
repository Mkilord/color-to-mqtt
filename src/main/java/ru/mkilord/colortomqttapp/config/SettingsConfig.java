package ru.mkilord.colortomqttapp.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.mkilord.colortomqttapp.service.SettingsService;
import ru.mkilord.colortomqttapp.service.impl.SettingsServiceImpl;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

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

    @Setter
    Map<String, String> defaultSettings;

    public Path getSettingsFilePath() {
        return Path.of(settingsFile).toAbsolutePath().normalize();
    }

    @Primary
    @Bean
    public Properties getProperties(SettingsService settingsService) {
        return settingsService.loadOrElseLoadDefault();
    }
}
