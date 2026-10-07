package ru.mkilord.colortomqttapp.service.impl;

import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import ru.mkilord.colortomqttapp.core.screenshoter.DefaultScreenShooter;
import ru.mkilord.colortomqttapp.core.screenshoter.ScreenShooter;
import ru.mkilord.colortomqttapp.service.RenderService;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(level = PRIVATE, makeFinal = true)
@Log4j2
public final class ScreenRenderServiceImpl implements RenderService {
    ScreenShooter screenShooter;

    public ScreenRenderServiceImpl(Properties properties) {
        this.screenShooter = new DefaultScreenShooter(properties);
    }

    @Override
    public byte[] getRenderedImage() {
        var image = screenShooter.getScreenshot();
        try (var outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Image render error!", e);
        }
    }
}
