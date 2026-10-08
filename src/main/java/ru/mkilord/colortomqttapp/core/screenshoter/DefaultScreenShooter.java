package ru.mkilord.colortomqttapp.core.screenshoter;

import lombok.experimental.FieldDefaults;
import ru.mkilord.colortomqttapp.core.AbstractFactory;
import ru.mkilord.colortomqttapp.core.screenshoter.screenArea.ScreenArea;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class DefaultScreenShooter implements ScreenShooter {

    Robot robot;
    Rectangle screenArea;

    public DefaultScreenShooter(Properties properties) {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
        var factory = new AbstractFactory<ScreenArea>();
        var screenArea = factory.get(ScreenArea.SCREEN_AREA_KEY, properties);
        this.screenArea = screenArea.getScreenArea();
    }

    @Override
    public BufferedImage getScreenshot() {
        return robot.createScreenCapture(screenArea);
    }
}
