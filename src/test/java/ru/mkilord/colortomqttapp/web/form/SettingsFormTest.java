package ru.mkilord.colortomqttapp.web.form;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.error.Violation;
import ru.mkilord.colortomqttapp.domain.settings.CaptureSettings;
import ru.mkilord.colortomqttapp.domain.settings.DetectionMethod;
import ru.mkilord.colortomqttapp.domain.settings.DetectionSettings;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class SettingsFormTest {

    private static final MqttConnection CONNECTION = new MqttConnection("tcp://h:1883", "t", "user", "secret");

    @Test
    void roundTrip() {
        var settings = ProfileSettings.DEFAULTS.toBuilder()
                .capture(CaptureSettings.builder().width(321).build())
                .detection(DetectionSettings.builder().method(DetectionMethod.VIVID).build())
                .build();

        var form = SettingsForm.of(settings, CONNECTION);

        assertThat(form.getPassword()).isNull();
        assertThat(form.isPasswordSet()).isTrue();
        assertThat(form.toProfileSettings()).isEqualTo(settings);
        assertThat(form.toConnection().password()).isEmpty();
    }

    @Test
    void violationsUseDomainPathsMappedToFormFields() {
        var form = SettingsForm.of(ProfileSettings.DEFAULTS, CONNECTION);
        form.setScreenWidth(0);
        form.setSensitivity(500);

        var error = catchThrowableOfType(SettingsValidationException.class, form::toProfileSettings);

        assertThat(error.violations()).extracting(Violation::field).extracting(SettingsForm::fieldOf)
                .contains("screenWidth", "sensitivity");
    }

    @Test
    void emptyFieldIsReported() {
        var form = SettingsForm.of(ProfileSettings.DEFAULTS, CONNECTION);
        form.setCellSize(null);

        var error = catchThrowableOfType(SettingsValidationException.class, form::toProfileSettings);

        assertThat(error.violations()).extracting(Violation::field).extracting(SettingsForm::fieldOf)
                .containsExactly("cellSize");
    }

    @Test
    void rangeOrderErrorPointsToUpperBound() {
        assertThat(SettingsForm.fieldOf("correction.ranges.saturation")).isEqualTo("maxSaturation");
        assertThat(SettingsForm.fieldOf("capture.framePeriodMs")).isEqualTo("updatePeriod");
    }
}
