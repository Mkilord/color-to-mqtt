package ru.mkilord.colortomqttapp.core.publishing;

import ru.mkilord.colortomqttapp.domain.color.HsbColor;

import java.util.Locale;

/**
 * Текст сообщения: {@code {"hue":210,"sat":64,"brightness":48}}, значения округлены до целых.
 */
public final class ColorPayload {

    private ColorPayload() {
    }

    public static String of(HsbColor color) {
        return String.format(Locale.ROOT, "{\"hue\":%.0f,\"sat\":%.0f,\"brightness\":%.0f}",
                color.hue(), color.saturation(), color.brightness());
    }
}
