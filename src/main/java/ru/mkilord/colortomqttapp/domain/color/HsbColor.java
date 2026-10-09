package ru.mkilord.colortomqttapp.domain.color;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

import java.awt.Color;

/**
 * Цвет в HSB: тон 0..360, насыщенность и яркость 0..100.
 */
public record HsbColor(float hue, float saturation, float brightness) {

    public static final HsbColor BLACK = new HsbColor(0, 0, 0);

    public HsbColor {
        if (!(hue >= 0 && hue <= 360 && saturation >= 0 && saturation <= 100 && brightness >= 0 && brightness <= 100)) {
            Checks.of("color")
                    .range("hue", hue, 0, 360)
                    .range("saturation", saturation, 0, 100)
                    .range("brightness", brightness, 0, 100)
                    .validate();
        }
    }

    public static HsbColor of(Color color) {
        var hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return new HsbColor(hsb[0] * 360, hsb[1] * 100, hsb[2] * 100);
    }
}
