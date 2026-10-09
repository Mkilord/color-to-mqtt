package ru.mkilord.colortomqttapp.domain.settings;

/**
 * Допустимый диапазон одной составляющей цвета. Границы проверяет {@link ColorRanges}:
 * у тона и у насыщенности с яркостью они разные.
 */
public record ColorRange(float min, float max) {

    public float clamp(float value) {
        return Math.min(Math.max(value, min), max);
    }
}
