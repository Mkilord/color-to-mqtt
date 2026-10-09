package ru.mkilord.colortomqttapp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mkilord.colortomqttapp.config.SettingsConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsServiceImplTest {

    @TempDir
    Path tempDir;

    private SettingsServiceImpl service;
    private Path settingsFile;

    @BeforeEach
    void setUp() {
        settingsFile = tempDir.resolve("settings.txt");
        service = new SettingsServiceImpl(config());
    }

    private SettingsConfig config() {
        var config = new SettingsConfig();
        config.setSettingsFile(settingsFile.toString());
        config.setProfilesDir(tempDir.resolve("profiles").toString());
        config.setDefaultSettings(Map.of(
                "broker", "tcp://localhost:1883",
                "topic", "colorToMQTT",
                "username", "",
                "password", "",
                "screenWight", "400",
                "maxBrightness", "100"));
        return config;
    }

    @Test
    void startsWithOneDefaultProfile() {
        assertThat(service.profiles()).containsExactly("Основной");
        assertThat(service.activeProfile()).isEqualTo("Основной");
        assertThat(service.loadOrElseLoadDefault().getProperty("screenWight")).isEqualTo("400");
    }

    @Test
    void oldSettingsFileIsSplitIntoGlobalAndProfile() throws IOException {
        Files.writeString(settingsFile, "broker=tcp\\://lamp\\:1883\nusername=user\npassword=secret\nscreenWight=800\n",
                StandardCharsets.UTF_8);

        var loaded = service.loadOrElseLoadDefault();

        assertThat(loaded.getProperty("broker")).isEqualTo("tcp://lamp:1883");
        assertThat(loaded.getProperty("password")).isEqualTo("secret");
        assertThat(loaded.getProperty("screenWight")).isEqualTo("800");
        assertThat(Files.readString(settingsFile)).doesNotContain("screenWight");
        assertThat(Files.readString(tempDir.resolve("profiles/Основной.properties"), StandardCharsets.UTF_8))
                .contains("screenWight").doesNotContain("password");
    }

    @Test
    void profilesKeepOwnSettingsAndShareConnection() {
        service.createProfile("Игры", "Основной");
        var games = service.loadProfile("Игры");
        games.setProperty("maxBrightness", "30");
        games.setProperty("broker", "tcp://other:1883");
        service.saveProfile("Игры", games);

        assertThat(service.loadProfile("Игры").getProperty("maxBrightness")).isEqualTo("30");
        assertThat(service.loadProfile("Основной").getProperty("maxBrightness")).isEqualTo("100");
        assertThat(service.loadProfile("Основной").getProperty("broker")).isEqualTo("tcp://other:1883");
    }

    @Test
    void newProfileCanStartFromDefaults() {
        var main = service.loadProfile("Основной");
        main.setProperty("maxBrightness", "40");
        service.saveProfile("Основной", main);

        service.createProfile("Ночь", null);

        assertThat(service.loadProfile("Ночь").getProperty("maxBrightness")).isEqualTo("100");
        assertThat(service.profiles()).containsExactly("Ночь", "Основной");
    }

    @Test
    void activeProfileIsRememberedAcrossInstances() {
        service.createProfile("Игры", null);
        service.activate("Игры");

        assertThat(new SettingsServiceImpl(config()).activeProfile()).isEqualTo("Игры");
    }

    @Test
    void renamingActiveProfileKeepsItActive() {
        service.renameProfile("Основной", "Фильмы");

        assertThat(service.profiles()).containsExactly("Фильмы");
        assertThat(service.activeProfile()).isEqualTo("Фильмы");
    }

    @Test
    void deletingActiveProfileActivatesAnother() {
        service.createProfile("Игры", null);
        service.activate("Игры");

        service.deleteProfile("Игры");

        assertThat(service.profiles()).containsExactly("Основной");
        assertThat(service.activeProfile()).isEqualTo("Основной");
    }

    @Test
    void lastProfileCannotBeDeleted() {
        assertThatThrownBy(() -> service.deleteProfile("Основной"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("последний");
    }

    @Test
    void duplicateAndInvalidNamesAreRejected() {
        service.createProfile("Игры", null);

        assertThatThrownBy(() -> service.createProfile("игры", null)).hasMessageContaining("уже есть");
        assertThatThrownBy(() -> service.createProfile("../evil", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createProfile("con", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createProfile("", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void renameOnlyChangingCaseIsAllowed() {
        service.createProfile("игры", null);

        service.renameProfile("игры", "Игры");

        assertThat(service.profiles()).contains("Игры");
    }

    @Test
    void resetProfileKeepsConnection() {
        var main = service.loadProfile("Основной");
        main.setProperty("maxBrightness", "20");
        main.setProperty("username", "lamp");
        service.saveProfile("Основной", main);

        var reset = service.resetProfile("Основной");

        assertThat(reset.getProperty("maxBrightness")).isEqualTo("100");
        assertThat(reset.getProperty("username")).isEqualTo("lamp");
    }

    @Test
    void unknownProfileIsRejected() {
        assertThatThrownBy(() -> service.loadProfile("Нет такого")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.saveProfile("Нет такого", new Properties())).isInstanceOf(IllegalArgumentException.class);
    }
}
