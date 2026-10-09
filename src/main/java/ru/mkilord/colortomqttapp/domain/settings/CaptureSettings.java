package ru.mkilord.colortomqttapp.domain.settings;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

/**
 * @param width         ширина области в центре экрана, px
 * @param height        высота области, px
 * @param cellSize      шаг сетки точек, px
 * @param sampling      порядок обхода точек
 * @param framePeriodMs пауза между кадрами, мс
 * @param idlePeriodMs  пауза между кадрами, если экран давно не меняется, мс; 0 отключает
 */
public record CaptureSettings(int width, int height, int cellSize, SamplingPattern sampling,
                              int framePeriodMs, int idlePeriodMs) {

    public static final CaptureSettings DEFAULTS = new CaptureSettings(400, 400, 20, SamplingPattern.CHESS, 10, 100);

    public CaptureSettings {
        Checks.of("capture")
                .range("width", width, 1, 10_000)
                .range("height", height, 1, 10_000)
                .range("cellSize", cellSize, 1, 1_000)
                .notNull("sampling", sampling)
                .range("framePeriodMs", framePeriodMs, 1, 10_000)
                .range("idlePeriodMs", idlePeriodMs, 0, 5_000)
                .validate();
    }

    /** Пауза в покое, не меньше обычной. */
    public int effectiveIdlePeriodMs() {
        return Math.max(framePeriodMs, idlePeriodMs);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder(DEFAULTS);
    }

    public static final class Builder {
        private int width;
        private int height;
        private int cellSize;
        private SamplingPattern sampling;
        private int framePeriodMs;
        private int idlePeriodMs;

        private Builder(CaptureSettings source) {
            width = source.width;
            height = source.height;
            cellSize = source.cellSize;
            sampling = source.sampling;
            framePeriodMs = source.framePeriodMs;
            idlePeriodMs = source.idlePeriodMs;
        }

        public Builder width(int value) {
            width = value;
            return this;
        }

        public Builder height(int value) {
            height = value;
            return this;
        }

        public Builder cellSize(int value) {
            cellSize = value;
            return this;
        }

        public Builder sampling(SamplingPattern value) {
            sampling = value;
            return this;
        }

        public Builder framePeriodMs(int value) {
            framePeriodMs = value;
            return this;
        }

        public Builder idlePeriodMs(int value) {
            idlePeriodMs = value;
            return this;
        }

        public CaptureSettings build() {
            return new CaptureSettings(width, height, cellSize, sampling, framePeriodMs, idlePeriodMs);
        }
    }
}
