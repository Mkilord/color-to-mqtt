package ru.mkilord.colortomqttapp.domain.settings;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

/**
 * @param holdTimeMs          новый цвет уходит, только если продержался столько, мс; 0 отключает
 * @param comparison          когда цвет считается новым
 * @param hueTolerance        допуск тона для {@link ComparisonMethod#HSB_TOLERANCE}, доля круга
 * @param saturationTolerance допуск насыщенности, доля
 * @param brightnessTolerance допуск яркости, доля
 * @param rgbThresholdPercent порог для {@link ComparisonMethod#RGB_DISTANCE}, % от 255
 */
public record SendingSettings(int holdTimeMs, ComparisonMethod comparison, float hueTolerance,
                              float saturationTolerance, float brightnessTolerance, int rgbThresholdPercent) {

    public static final SendingSettings DEFAULTS =
            new SendingSettings(150, ComparisonMethod.HSB_TOLERANCE, 0.2f, 0.15f, 0.06f, 30);

    public SendingSettings {
        Checks.of("sending")
                .range("holdTimeMs", holdTimeMs, 0, 5_000)
                .notNull("comparison", comparison)
                .range("hueTolerance", hueTolerance, 0, 1)
                .range("saturationTolerance", saturationTolerance, 0, 1)
                .range("brightnessTolerance", brightnessTolerance, 0, 1)
                .range("rgbThresholdPercent", rgbThresholdPercent, 0, 100)
                .validate();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(DEFAULTS);
    }

    public static final class Builder {
        private int holdTimeMs;
        private ComparisonMethod comparison;
        private float hueTolerance;
        private float saturationTolerance;
        private float brightnessTolerance;
        private int rgbThresholdPercent;

        private Builder(SendingSettings source) {
            holdTimeMs = source.holdTimeMs;
            comparison = source.comparison;
            hueTolerance = source.hueTolerance;
            saturationTolerance = source.saturationTolerance;
            brightnessTolerance = source.brightnessTolerance;
            rgbThresholdPercent = source.rgbThresholdPercent;
        }

        public Builder holdTimeMs(int value) {
            holdTimeMs = value;
            return this;
        }

        public Builder comparison(ComparisonMethod value) {
            comparison = value;
            return this;
        }

        public Builder hueTolerance(float value) {
            hueTolerance = value;
            return this;
        }

        public Builder saturationTolerance(float value) {
            saturationTolerance = value;
            return this;
        }

        public Builder brightnessTolerance(float value) {
            brightnessTolerance = value;
            return this;
        }

        public Builder rgbThresholdPercent(int value) {
            rgbThresholdPercent = value;
            return this;
        }

        public SendingSettings build() {
            return new SendingSettings(holdTimeMs, comparison, hueTolerance, saturationTolerance,
                    brightnessTolerance, rgbThresholdPercent);
        }
    }
}
