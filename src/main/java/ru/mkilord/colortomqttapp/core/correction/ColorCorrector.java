package ru.mkilord.colortomqttapp.core.correction;

import ru.mkilord.colortomqttapp.domain.color.HsbColor;
import ru.mkilord.colortomqttapp.domain.settings.CorrectionSettings;

/**
 * Поправка тона по опорным цветам, усиление насыщенности, сдвиг и ограничение диапазонами.
 */
public final class ColorCorrector {

    private final CorrectionSettings settings;
    private final float[] hueShifts;

    public ColorCorrector(CorrectionSettings settings) {
        this.settings = settings;
        this.hueShifts = settings.hueShifts().toArray();
    }

    /** Цвет после поправок, но до ограничения диапазонами. */
    public HsbColor adjust(HsbColor color) {
        var hue = rotate(mapHue(color.hue(), hueShifts), settings.hueShift());
        var saturation = clamp(boost(color.saturation(), settings.saturationBoost()) + settings.saturationShift());
        var brightness = clamp(color.brightness() + settings.brightnessShift());
        return new HsbColor(hue, saturation, brightness);
    }

    public HsbColor limit(HsbColor color) {
        var ranges = settings.ranges();
        return new HsbColor(ranges.hue().clamp(color.hue()), ranges.saturation().clamp(color.saturation()),
                ranges.brightness().clamp(color.brightness()));
    }

    public HsbColor apply(HsbColor color) {
        return limit(adjust(color));
    }

    /**
     * Приближает насыщенность к 100 на {@code boost} процентов оставшегося: при 40% 65 становится 79.
     * Средний цвет области бледнее экрана, а лампы при неполной насыщенности выглядят еще бледнее.
     */
    static float boost(float saturation, float boost) {
        return saturation + (100 - saturation) * boost / 100;
    }

    /**
     * Поправка тона по шести опорным цветам с линейным переходом между ними. Светодиоды ламп
     * передают цвета не так, как монитор: поправка для зеленого не трогает красный и синий.
     */
    static float mapHue(float hue, float[] shifts) {
        var h = rotate(hue, 0);
        var sector = Math.min((int) (h / 60), 5);
        var t = (h - sector * 60) / 60;
        var shift = shifts[sector] * (1 - t) + shifts[(sector + 1) % 6] * t;
        return rotate(h, shift);
    }

    static float rotate(float hue, float shift) {
        var result = (hue + shift) % 360;
        return result < 0 ? result + 360 : result;
    }

    private static float clamp(float value) {
        return Math.min(Math.max(value, 0), 100);
    }
}
