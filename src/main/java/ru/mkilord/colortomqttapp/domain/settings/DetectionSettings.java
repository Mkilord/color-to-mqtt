package ru.mkilord.colortomqttapp.domain.settings;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

/**
 * @param method          способ расчета цвета кадра
 * @param minSharePercent для преобладающего цвета: меньшая доля цветных точек считается отсутствием цвета, %
 * @param blackThreshold  кадр темнее считается черным, % яркости; 0 отключает
 * @param grayThreshold   кадр бледнее считается серым, % насыщенности; 0 отключает
 */
public record DetectionSettings(DetectionMethod method, float minSharePercent,
                                float blackThreshold, float grayThreshold) {

    public static final DetectionSettings DEFAULTS = new DetectionSettings(DetectionMethod.DOMINANT, 5, 5, 12);

    public DetectionSettings {
        Checks.of("detection")
                .notNull("method", method)
                .range("minSharePercent", minSharePercent, 0, 100)
                .range("blackThreshold", blackThreshold, 0, 50)
                .range("grayThreshold", grayThreshold, 0, 100)
                .validate();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(DEFAULTS);
    }

    public static final class Builder {
        private DetectionMethod method;
        private float minSharePercent;
        private float blackThreshold;
        private float grayThreshold;

        private Builder(DetectionSettings source) {
            method = source.method;
            minSharePercent = source.minSharePercent;
            blackThreshold = source.blackThreshold;
            grayThreshold = source.grayThreshold;
        }

        public Builder method(DetectionMethod value) {
            method = value;
            return this;
        }

        public Builder minSharePercent(float value) {
            minSharePercent = value;
            return this;
        }

        public Builder blackThreshold(float value) {
            blackThreshold = value;
            return this;
        }

        public Builder grayThreshold(float value) {
            grayThreshold = value;
            return this;
        }

        public DetectionSettings build() {
            return new DetectionSettings(method, minSharePercent, blackThreshold, grayThreshold);
        }
    }
}
