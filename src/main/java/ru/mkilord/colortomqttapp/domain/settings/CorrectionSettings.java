package ru.mkilord.colortomqttapp.domain.settings;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

/**
 * Как цвет экрана переводится в цвет ламп. Применяется только к цветным кадрам.
 *
 * @param saturationBoost приближает насыщенность к 100 на эту долю оставшегося, %
 * @param hueShift        сдвиг тона по кругу, градусы
 * @param saturationShift сдвиг насыщенности, %
 * @param brightnessShift сдвиг яркости, %
 * @param hueShifts       поправка тона по опорным цветам
 * @param ranges          допустимые диапазоны после коррекции
 */
public record CorrectionSettings(float saturationBoost, float hueShift, float saturationShift, float brightnessShift,
                                 HueShifts hueShifts, ColorRanges ranges) {

    public static final CorrectionSettings DEFAULTS =
            new CorrectionSettings(30, 0, 0, 0, HueShifts.NONE, ColorRanges.FULL);

    public CorrectionSettings {
        Checks.of("correction")
                .range("saturationBoost", saturationBoost, 0, 100)
                .range("hueShift", hueShift, -360, 360)
                .range("saturationShift", saturationShift, -100, 100)
                .range("brightnessShift", brightnessShift, -100, 100)
                .notNull("hueShifts", hueShifts)
                .notNull("ranges", ranges)
                .validate();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(DEFAULTS);
    }

    public static final class Builder {
        private float saturationBoost;
        private float hueShift;
        private float saturationShift;
        private float brightnessShift;
        private HueShifts hueShifts;
        private ColorRanges ranges;

        private Builder(CorrectionSettings source) {
            saturationBoost = source.saturationBoost;
            hueShift = source.hueShift;
            saturationShift = source.saturationShift;
            brightnessShift = source.brightnessShift;
            hueShifts = source.hueShifts;
            ranges = source.ranges;
        }

        public Builder saturationBoost(float value) {
            saturationBoost = value;
            return this;
        }

        public Builder hueShift(float value) {
            hueShift = value;
            return this;
        }

        public Builder saturationShift(float value) {
            saturationShift = value;
            return this;
        }

        public Builder brightnessShift(float value) {
            brightnessShift = value;
            return this;
        }

        public Builder hueShifts(HueShifts value) {
            hueShifts = value;
            return this;
        }

        public Builder ranges(ColorRanges value) {
            ranges = value;
            return this;
        }

        public CorrectionSettings build() {
            return new CorrectionSettings(saturationBoost, hueShift, saturationShift, brightnessShift, hueShifts, ranges);
        }
    }
}
