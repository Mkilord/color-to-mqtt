package ru.mkilord.colortomqttapp.controller.form;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.TestProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsFormTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void defaultsAreValid() {
        assertThat(validator.validate(SettingsForm.from(TestProperties.defaults()))).isEmpty();
    }

    @Test
    void rejectsInvertedRangeAndUnknownTracker() {
        var form = SettingsForm.from(TestProperties.defaults());
        form.setMinBrightness(80f);
        form.setMaxBrightness(20f);
        form.setStateTracker("java.lang.String");

        var messages = validator.validate(form).stream().map(v -> v.getMessage()).toList();

        assertThat(messages).contains("Минимум яркости больше максимума", "Неизвестный способ сравнения");
    }

    @Test
    void rejectsBrokerWithoutScheme() {
        var form = SettingsForm.from(TestProperties.defaults());
        form.setBroker("localhost:1883");

        assertThat(validator.validate(form)).extracting(v -> v.getPropertyPath().toString()).containsExactly("broker");
    }

    @Test
    void roundTripKeepsValuesUnderOriginalKeys() {
        var form = SettingsForm.from(TestProperties.defaults());
        form.setScreenWidth(800);
        form.setModifyHue(-15f);
        var props = new Properties();

        form.applyTo(props);

        assertThat(props.getProperty("screenWight")).isEqualTo("800");
        assertThat(props.getProperty("modifyHue")).isEqualTo("-15.0");
        assertThat(SettingsForm.from(props)).isEqualTo(form);
    }

    @Test
    void passwordIsNeverExposedButItsPresenceIs() {
        var props = TestProperties.defaults();
        props.setProperty("username", "lamp");
        props.setProperty("password", "secret");

        var form = SettingsForm.from(props);

        assertThat(form.getUsername()).isEqualTo("lamp");
        assertThat(form.getPassword()).isNullOrEmpty();
        assertThat(form.isPasswordSet()).isTrue();
    }

    @Test
    void emptyPasswordKeepsSavedOne() {
        var props = TestProperties.defaults();
        props.setProperty("username", "lamp");
        props.setProperty("password", "secret");
        var form = SettingsForm.from(props);
        form.setPassword("");

        form.applyTo(props);

        assertThat(props.getProperty("password")).isEqualTo("secret");
    }

    @Test
    void newPasswordReplacesSavedOne() {
        var props = TestProperties.defaults();
        props.setProperty("username", "lamp");
        props.setProperty("password", "secret");
        var form = SettingsForm.from(props);
        form.setPassword("other");

        form.applyTo(props);

        assertThat(props.getProperty("password")).isEqualTo("other");
    }

    @Test
    void clearingUsernameDropsPassword() {
        var props = TestProperties.defaults();
        props.setProperty("username", "lamp");
        props.setProperty("password", "secret");
        var form = SettingsForm.from(props);
        form.setUsername("  ");

        form.applyTo(props);

        assertThat(props.getProperty("username")).isEmpty();
        assertThat(props.getProperty("password")).isEmpty();
    }

    @Test
    void zoneThresholdsFallBackToDefaultsForOldSettingsFile() {
        var props = TestProperties.defaults();
        props.remove("blackThreshold");
        props.remove("grayThreshold");

        var form = SettingsForm.from(props);

        assertThat(form.getBlackThreshold()).isEqualTo(5f);
        assertThat(form.getGrayThreshold()).isEqualTo(12f);
    }
}
