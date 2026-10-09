package ru.mkilord.colortomqttapp.domain;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.error.Violation;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.CaptureSettings;
import ru.mkilord.colortomqttapp.domain.settings.ColorRange;
import ru.mkilord.colortomqttapp.domain.settings.ColorRanges;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;
import ru.mkilord.colortomqttapp.domain.settings.SendingSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class SettingsValidationTest {

    @Test
    void defaultsAreValid() {
        assertThat(ProfileSettings.DEFAULTS.capture().width()).isEqualTo(400);
        assertThat(ProfileSettings.builder().build()).isEqualTo(ProfileSettings.DEFAULTS);
    }

    @Test
    void allViolationsOfOneObjectAreReportedAtOnce() {
        var error = catchThrowableOfType(SettingsValidationException.class,
                () -> CaptureSettings.builder().width(0).cellSize(5000).framePeriodMs(0).build());

        assertThat(error.violations()).extracting(Violation::field)
                .containsExactly("capture.width", "capture.cellSize", "capture.framePeriodMs");
        assertThat(error.violations().get(0).message()).isEqualTo("От 1 до 10000");
    }

    @Test
    void builderChangesOnlyGivenValues() {
        var changed = SendingSettings.DEFAULTS.toBuilder().holdTimeMs(300).build();

        assertThat(changed.holdTimeMs()).isEqualTo(300);
        assertThat(changed.comparison()).isEqualTo(SendingSettings.DEFAULTS.comparison());
    }

    @Test
    void decimalLimitsAreFormattedReadably() {
        var error = catchThrowableOfType(SettingsValidationException.class,
                () -> SendingSettings.builder().hueTolerance(5).build());

        assertThat(error.violations()).containsExactly(new Violation("sending.hueTolerance", "От 0 до 1"));
    }

    @Test
    void invertedRangeIsReportedOnTheRange() {
        var error = catchThrowableOfType(SettingsValidationException.class, () -> new ColorRanges(
                new ColorRange(0, 360), new ColorRange(0, 100), new ColorRange(80, 20)));

        assertThat(error.violations()).containsExactly(
                new Violation("correction.ranges.brightness", "Нижняя граница больше верхней"));
    }

    @Test
    void connectionRequiresSchemeAndPlainTopic() {
        var error = catchThrowableOfType(SettingsValidationException.class,
                () -> new MqttConnection("localhost:1883", "home/#", "", ""));

        assertThat(error.violations()).extracting(Violation::field)
                .containsExactly("connection.broker", "connection.topic");
    }

    @Test
    void passwordWithoutLoginIsDropped() {
        assertThat(new MqttConnection("tcp://h:1", "t", " ", "secret").password()).isEmpty();
    }

    @Test
    void emptyPasswordKeepsSavedOneWhileLoginIsSet() {
        var saved = new MqttConnection("tcp://h:1", "t", "lamp", "secret");

        assertThat(saved.merge(new MqttConnection("tcp://h:1", "t", "lamp", "")).password()).isEqualTo("secret");
        assertThat(saved.merge(new MqttConnection("tcp://h:1", "t", "lamp", "new")).password()).isEqualTo("new");
        assertThat(saved.merge(new MqttConnection("tcp://h:1", "t", "", "")).password()).isEmpty();
    }

    @Test
    void profileNameRejectsPathsAndReservedNames() {
        assertThat(ProfileName.of("  Игры ").value()).isEqualTo("Игры");
        assertThatThrownBy(() -> ProfileName.of("../evil")).isInstanceOf(SettingsValidationException.class);
        assertThatThrownBy(() -> ProfileName.of("con")).isInstanceOf(SettingsValidationException.class);
        assertThatThrownBy(() -> ProfileName.of("")).isInstanceOf(SettingsValidationException.class);
        assertThat(ProfileName.of("Игры").sameFileAs(ProfileName.of("игры"))).isTrue();
    }
}
