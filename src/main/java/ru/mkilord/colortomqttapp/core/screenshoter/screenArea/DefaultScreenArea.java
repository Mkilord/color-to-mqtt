package ru.mkilord.colortomqttapp.core.screenshoter.screenArea;

import lombok.experimental.FieldDefaults;

import java.awt.*;
import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class DefaultScreenArea implements ScreenArea {
    public static final String SCREEN_HEIGHT_KEY = "screenHeight";
    public static final String SCREEN_WIGHT_KEY = "screenWight";

    Dimension size;

    public DefaultScreenArea(Properties properties) {
        var height = Integer.parseInt(properties.getProperty(SCREEN_HEIGHT_KEY));
        var width = Integer.parseInt(properties.getProperty(SCREEN_WIGHT_KEY));
        this.size = new Dimension(width, height);
    }

    @Override
    public Rectangle getScreenArea() {
        return centered(Toolkit.getDefaultToolkit().getScreenSize(), size);
    }

    /**
     * Прямоугольник заданного размера по центру экрана. Если область больше экрана,
     * она обрезается до его размеров.
     */
    static Rectangle centered(Dimension screen, Dimension area) {
        var width = Math.min(area.width, screen.width);
        var height = Math.min(area.height, screen.height);
        return new Rectangle((screen.width - width) / 2, (screen.height - height) / 2, width, height);
    }
}
