package ru.mkilord.colortomqttapp.core;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Тестовые кадры.
 */
public final class Frames {

    private Frames() {
    }

    public static BufferedImage filled(int width, int height, Color color) {
        return square(width, height, color, color, 0, 0);
    }

    /** Фон и квадрат [from, to) по обеим осям. */
    public static BufferedImage square(int width, int height, Color background, Color square, int from, int to) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                var inside = x >= from && x < to && y >= from && y < to;
                image.setRGB(x, y, (inside ? square : background).getRGB());
            }
        }
        return image;
    }
}
