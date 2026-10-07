package ru.mkilord.colortomqttapp.service.impl;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import ru.mkilord.colortomqttapp.config.SettingsConfig;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@Log4j2
@Component
@AllArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class SettingsServiceImpl implements SettingsService {

    SettingsConfig config;

    @Override
    public void save(Properties editedProperties) {
        var propertiesFile = config.getSettingsFilePath();
        var toSave = new Properties();
        toSave.putAll(editedProperties);

        try (var fos = new OutputStreamWriter(new FileOutputStream(propertiesFile.toFile()), StandardCharsets.UTF_8)) {
            toSave.store(fos, "Application settings");
            log.debug("Settings saved to {}", propertiesFile);
        } catch (IOException e) {
            log.error("Failed to save settings: {}", propertiesFile, e);
        }
    }

    @Override
    public Properties load() throws IOException {
        var propertiesFile = config.getSettingsFilePath();

        try (var reader = new InputStreamReader(new FileInputStream(propertiesFile.toFile()), StandardCharsets.UTF_8)) {
            var properties = new Properties();
            properties.load(reader);
            log.debug("Loaded properties from {}", propertiesFile);
            return properties;
        }
    }

    /**
     * Настройки по умолчанию, поверх которых применены сохраненные в файле.
     */
    @Override
    public Properties loadOrElseLoadDefault() {
        var settingsFilePath = config.getSettingsFilePath();
        var properties = loadDefault();
        log.debug("Loading properties from file {}", settingsFilePath);
        try {
            properties.putAll(load());
        } catch (IOException e) {
            log.warn("Could not load properties from file {}, using defaults", settingsFilePath);
        }
        return properties;
    }

    @Override
    public Properties resetToDefaults() {
        var propertiesFile = config.getSettingsFilePath();
        try {
            Files.deleteIfExists(propertiesFile);
        } catch (IOException e) {
            log.error("Failed to delete settings file: {}", propertiesFile, e);
        }
        return loadDefault();
    }

    @Override
    public Properties loadDefault() {
        var properties = new Properties();
        properties.putAll(config.getDefaultSettings());
        log.debug("Loaded default properties!");
        return properties;
    }
}
