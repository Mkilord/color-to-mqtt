package ru.mkilord.colortomqttapp.infrastructure.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.CaptureSettings;
import ru.mkilord.colortomqttapp.domain.settings.ComparisonMethod;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;
import ru.mkilord.colortomqttapp.domain.settings.SendingSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YamlSettingsStoreTest {

    private static final MqttConnection DEFAULT_CONNECTION =
            new MqttConnection("tcp://localhost:1883", "screen/color", "", "");
    private static final ProfileName GAMES = ProfileName.of("Игры");

    @TempDir
    Path root;

    private YamlSettingsStore store() {
        return new YamlSettingsStore(root, DEFAULT_CONNECTION);
    }

    @Test
    void emptyFolderHasNoProfilesAndDefaultConnection() {
        var store = store();

        assertThat(store.profileNames()).isEmpty();
        assertThat(store.loadActiveProfile()).isEmpty();
        assertThat(store.loadConnection()).isEqualTo(DEFAULT_CONNECTION);
    }

    @Test
    void defaultProfileIsStoredAsEmptyFile() throws IOException {
        store().saveProfile(ProfileName.DEFAULT, ProfileSettings.DEFAULTS);

        assertThat(Files.readString(root.resolve("profiles/Основной.yaml")).strip()).isIn("{}", "");
        assertThat(store().loadProfile(ProfileName.DEFAULT)).contains(ProfileSettings.DEFAULTS);
    }

    @Test
    void partialNestedValuesKeepOtherDefaults() throws IOException {
        Files.createDirectories(root.resolve("profiles"));
        Files.writeString(root.resolve("profiles/Игры.yaml"), """
                correction:
                  ranges:
                    brightness:
                      max: 50
                sending:
                  comparison: RGB_DISTANCE
                """);

        var games = store().loadProfile(GAMES).orElseThrow();

        assertThat(games.correction().ranges().brightness().max()).isEqualTo(50);
        assertThat(games.correction().ranges().brightness().min()).isEqualTo(0);
        assertThat(games.sending().comparison()).isEqualTo(ComparisonMethod.RGB_DISTANCE);
        assertThat(games.sending().holdTimeMs()).isEqualTo(SendingSettings.DEFAULTS.holdTimeMs());
    }

    @Test
    void profileFileKeepsOnlyDifferences() throws IOException {
        var store = store();
        var settings = ProfileSettings.DEFAULTS.toBuilder()
                .capture(CaptureSettings.builder().width(800).build())
                .build();

        store.saveProfile(GAMES, settings);

        var text = Files.readString(root.resolve("profiles/Игры.yaml"));
        assertThat(text).contains("width: 800").doesNotContain("height").doesNotContain("sending");
        assertThat(store.loadProfile(GAMES)).contains(settings);
    }

    @Test
    void renameAndDeleteMoveFiles() {
        var store = store();
        store.saveProfile(ProfileName.DEFAULT, ProfileSettings.DEFAULTS);
        store.saveProfile(GAMES, ProfileSettings.DEFAULTS);

        store.renameProfile(GAMES, ProfileName.of("Кино"));
        assertThat(store.profileNames()).containsExactly(ProfileName.of("Кино"), ProfileName.DEFAULT);

        store.deleteProfile(ProfileName.of("Кино"));
        assertThat(store.profileNames()).containsExactly(ProfileName.DEFAULT);
    }

    @Test
    void connectionRoundTrip() {
        var store = store();
        var connection = new MqttConnection("ssl://broker:8883", "home/lamps", "user", "secret");

        store.saveConnection(connection);

        assertThat(store().loadConnection()).isEqualTo(connection);
    }

    @Test
    void corruptedProfileIsReported() throws IOException {
        var store = store();
        Files.createDirectories(root.resolve("profiles"));
        Files.writeString(root.resolve("profiles/Игры.yaml"), "capture:\n  width: -5\n");

        assertThatThrownBy(() -> store.loadProfile(GAMES))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Игры");
    }
}
