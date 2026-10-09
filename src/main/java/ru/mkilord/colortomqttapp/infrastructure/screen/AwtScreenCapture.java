package ru.mkilord.colortomqttapp.infrastructure.screen;

import org.springframework.stereotype.Component;
import ru.mkilord.colortomqttapp.core.capture.ScreenCapture;
import ru.mkilord.colortomqttapp.domain.error.UnavailableException;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

/**
 * Снимок рабочего стола через {@link Robot}. Robot создается при первом снимке:
 * без графической среды приложение запускается, но захват недоступен.
 */
@Component
public class AwtScreenCapture implements ScreenCapture {

    private volatile Robot robot;

    @Override
    public Dimension screenSize() {
        if (GraphicsEnvironment.isHeadless()) {
            return null;
        }
        try {
            return Toolkit.getDefaultToolkit().getScreenSize();
        } catch (HeadlessException e) {
            return null;
        }
    }

    @Override
    public BufferedImage capture(Rectangle area) {
        return robot().createScreenCapture(area);
    }

    private Robot robot() {
        var current = robot;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (robot == null) {
                try {
                    robot = new Robot();
                } catch (AWTException | HeadlessException e) {
                    throw new UnavailableException("Захват экрана работает только на рабочем столе с графической оболочкой");
                }
            }
            return robot;
        }
    }
}
