package ru.mkilord.colortomqttapp.core.modifier;

import lombok.experimental.FieldDefaults;
import ru.mkilord.colortomqttapp.core.HSBColor;

import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DefaultColorModifier implements ColorModifier {

    public static final String MODIFY_HUE_KEY = "modifyHue";
    public static final String MODIFY_SATURATION_KEY = "modifySaturation";
    public static final String MODIFY_BRIGHTNESS_KEY = "modifyBrightness";
    public static final String SATURATION_BOOST_KEY = "saturationBoost";

    float modifyHue;
    float modifySaturation;
    float modifyBrightness;
    float saturationBoost;

    public DefaultColorModifier(Properties properties) {
        this.modifyHue = Float.parseFloat(properties.getProperty(MODIFY_HUE_KEY));
        this.modifySaturation = Float.parseFloat(properties.getProperty(MODIFY_SATURATION_KEY));
        this.modifyBrightness = Float.parseFloat(properties.getProperty(MODIFY_BRIGHTNESS_KEY));
        this.saturationBoost = Float.parseFloat(properties.getProperty(SATURATION_BOOST_KEY, "0"));
    }

    private float applyModifyFor(float value, float modification, float max) {
        float modifiedValue = value + modification;
        return Math.min(Math.max(0, modifiedValue), max);
    }

    /**
     * Тон круговой: 350 + 20 дает 10, а не 360.
     */
    private static float shiftHue(float hue, float shift) {
        float shifted = (hue + shift) % 360;
        return shifted < 0 ? shifted + 360 : shifted;
    }

    /**
     * Приближает насыщенность к 100 на {@code boost} процентов оставшегося: при 40% 65 становится 79.
     * Средний цвет области всегда бледнее экрана, а лампы при неполной насыщенности
     * выглядят еще бледнее, поэтому красный без усиления превращается в коралловый.
     */
    static float boostSaturation(float saturation, float boost) {
        return saturation + (100 - saturation) * boost / 100;
    }

    @Override
    public HSBColor modify(HSBColor color) {
        var h = shiftHue(color.getHue(), modifyHue);
        var s = applyModifyFor(boostSaturation(color.getSaturation(), saturationBoost), modifySaturation, 100);
        var b = applyModifyFor(color.getBrightness(), modifyBrightness, 100);
        return new HSBColor(h, s, b);
    }
}
