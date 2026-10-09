package ru.mkilord.colortomqttapp.infrastructure.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.CaptureSettings;
import ru.mkilord.colortomqttapp.domain.settings.ComparisonMethod;
import ru.mkilord.colortomqttapp.domain.settings.DetectionMethod;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;
import ru.mkilord.colortomqttapp.domain.settings.SamplingPattern;

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
    void emptyFolderGetsDefaultProfile() {
        var store = store();

        assertThat(store.profileNames()).containsExactly(ProfileName.DEFAULT);
        assertThat(store.loadActiveProfile()).contains(ProfileName.DEFAULT);
        assertThat(store.loadProfile(ProfileName.DEFAULT)).contains(ProfileSettings.DEFAULTS);
        assertThat(store.loadConnection()).isEqualTo(DEFAULT_CONNECTION);
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
        Files.writeString(root.resolve("profiles/Игры.yaml"), "capture:\n  width: -5\n");

        assertThatThrownBy(() -> store.loadProfile(GAMES))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Игры");
    }

    @Test
    void legacySettingsAreImported() throws IOException {
        Files.writeString(root.resolve("settings.txt"), """
                broker=tcp://192.168.1.5:1883
                topic=lamps/color
                username=ha
                password=pass
                activeProfile=Кино
                """);
        Files.createDirectories(root.resolve("profiles"));
        Files.writeString(root.resolve("profiles/Кино.properties"), """
                screenWight=640
                screenHeight=360
                processor=ru.mkilord.colortomqttapp.core.processor.GridProcessor
                detector=ru.mkilord.colortomqttapp.core.detector.VividColorDetector
                stateTracker=ru.mkilord.colortomqttapp.core.tracker.DefaultColorStateTracker
                sensitivity=25
                minHUE=10
                maxHUE=300
                hueShiftGreen=-15
                holdTime=200
                """);
        Files.writeString(root.resolve("profiles/Игры.properties"), "screenWight=999999\nholdTime=50\n");

        var store = store();

        assertThat(store.loadConnection()).isEqualTo(new MqttConnection("tcp://192.168.1.5:1883", "lamps/color", "ha", "pass"));
        assertThat(store.loadActiveProfile()).contains(ProfileName.of("Кино"));

        var movie = store.loadProfile(ProfileName.of("Кино")).orElseThrow();
        assertThat(movie.capture().width()).isEqualTo(640);
        assertThat(movie.capture().sampling()).isEqualTo(SamplingPattern.GRID);
        assertThat(movie.detection().method()).isEqualTo(DetectionMethod.VIVID);
        assertThat(movie.sending().comparison()).isEqualTo(ComparisonMethod.RGB_DISTANCE);
        assertThat(movie.sending().rgbThresholdPercent()).isEqualTo(25);
        assertThat(movie.sending().holdTimeMs()).isEqualTo(200);
        assertThat(movie.correction().ranges().hue().min()).isEqualTo(10);
        assertThat(movie.correction().hueShifts().green()).isEqualTo(-15);

        var games = store.loadProfile(GAMES).orElseThrow();
        assertThat(games.capture()).isEqualTo(CaptureSettings.DEFAULTS);
        assertThat(games.sending().holdTimeMs()).isEqualTo(50);

        assertThat(root.resolve("settings.txt.bak")).exists();
        assertThat(root.resolve("profiles/Кино.properties.bak")).exists();
        assertThat(root.resolve("settings.txt")).doesNotExist();
    }

    @Test
    void legacyGlobalSettingsBecomeDefaultProfile() throws IOException {
        Files.writeString(root.resolve("settings.txt"), "screenWight=500\nbroker=tcp://h:1883\ntopic=t\n");

        var store = store();

        assertThat(store.profileNames()).containsExactly(ProfileName.DEFAULT);
        assertThat(store.loadProfile(ProfileName.DEFAULT).orElseThrow().capture().width()).isEqualTo(500);
    }
}
