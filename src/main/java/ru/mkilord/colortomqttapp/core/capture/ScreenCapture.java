package ru.mkilord.colortomqttapp.core.capture;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Источник кадров. Реализация для рабочего стола снимает экран через AWT.
 */
public interface ScreenCapture {

    /** Размер экрана или null, если графической среды нет. */
    Dimension screenSize();

    BufferedImage capture(Rectangle area);

    /**
     * Прямоугольник заданного размера по центру экрана. Если он больше экрана, обрезается.
     */
    static Rectangle centered(Dimension screen, int width, int height) {
        var w = Math.min(width, screen.width);
        var h = Math.min(height, screen.height);
        return new Rectangle((screen.width - w) / 2, (screen.height - h) / 2, w, h);
    }
}
