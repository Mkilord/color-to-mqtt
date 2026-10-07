package ru.mkilord.colortomqttapp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mkilord.colortomqttapp.config.SettingsConfig;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsServiceImplTest {

    @TempDir
    Path tempDir;

    private SettingsServiceImpl service;
    private Path settingsFile;

    @BeforeEach
    void setUp() {
        settingsFile = tempDir.resolve("settings.txt");
        var config = new SettingsConfig();
        config.setSettingsFile(settingsFile.toString());
        config.setDefaultSettings(Map.of(
                "broker", "tcp://localhost:1883",
                "topic", "colorToMQTT",
                "username", "user",
                "password", "secret"));
        service = new SettingsServiceImpl(config);
    }

    @Test
    void returnsDefaultsWhenNoFile() {
        assertThat(service.loadOrElseLoadDefault().getProperty("topic")).isEqualTo("colorToMQTT");
    }

    @Test
    void savedValuesOverrideDefaultsIncludingCredentials() {
        var props = service.loadOrElseLoadDefault();
        props.setProperty("topic", "лампа/цвет");
        props.setProperty("username", "lamp");
        props.setProperty("password", "changed");

        service.save(props);

        var loaded = service.loadOrElseLoadDefault();
        assertThat(loaded.getProperty("topic")).isEqualTo("лампа/цвет");
        assertThat(loaded.getProperty("username")).isEqualTo("lamp");
        assertThat(loaded.getProperty("password")).isEqualTo("changed");
        assertThat(loaded.getProperty("broker")).isEqualTo("tcp://localhost:1883");
    }

    @Test
    void resetDeletesSavedFile() {
        var props = service.loadOrElseLoadDefault();
        props.setProperty("topic", "other");
        service.save(props);

        var reset = service.resetToDefaults();

        assertThat(settingsFile).doesNotExist();
        assertThat(reset.getProperty("topic")).isEqualTo("colorToMQTT");
    }
}
