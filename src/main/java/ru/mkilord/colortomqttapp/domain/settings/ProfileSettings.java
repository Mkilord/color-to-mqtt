package ru.mkilord.colortomqttapp.domain.settings;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

/**
 * Все настройки захвата и обработки цвета одного профиля.
 */
public record ProfileSettings(CaptureSettings capture, DetectionSettings detection,
                              CorrectionSettings correction, SendingSettings sending) {

    public static final ProfileSettings DEFAULTS = new ProfileSettings(
            CaptureSettings.DEFAULTS, DetectionSettings.DEFAULTS, CorrectionSettings.DEFAULTS, SendingSettings.DEFAULTS);

    public ProfileSettings {
        Checks.of("")
                .notNull("capture", capture)
                .notNull("detection", detection)
                .notNull("correction", correction)
                .notNull("sending", sending)
                .validate();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(DEFAULTS);
    }

    public static final class Builder {
        private CaptureSettings capture;
        private DetectionSettings detection;
        private CorrectionSettings correction;
        private SendingSettings sending;

        private Builder(ProfileSettings source) {
            capture = source.capture;
            detection = source.detection;
            correction = source.correction;
            sending = source.sending;
        }

        public Builder capture(CaptureSettings value) {
            capture = value;
            return this;
        }

        public Builder detection(DetectionSettings value) {
            detection = value;
            return this;
        }

        public Builder correction(CorrectionSettings value) {
            correction = value;
            return this;
        }

        public Builder sending(SendingSettings value) {
            sending = value;
            return this;
        }

        public ProfileSettings build() {
            return new ProfileSettings(capture, detection, correction, sending);
        }
    }
}
