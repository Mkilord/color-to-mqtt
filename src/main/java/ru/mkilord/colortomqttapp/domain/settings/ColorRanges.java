package ru.mkilord.colortomqttapp.domain.settings;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

public record ColorRanges(ColorRange hue, ColorRange saturation, ColorRange brightness) {

    public static final ColorRanges FULL = new ColorRanges(
            new ColorRange(0, 360), new ColorRange(0, 100), new ColorRange(0, 100));

    public ColorRanges {
        var checks = Checks.of("correction.ranges")
                .notNull("hue", hue)
                .notNull("saturation", saturation)
                .notNull("brightness", brightness);
        checks.validate();
        check(checks, "hue", hue, 360);
        check(checks, "saturation", saturation, 100);
        check(checks, "brightness", brightness, 100);
        checks.validate();
    }

    private static void check(Checks checks, String name, ColorRange range, float limit) {
        checks.range(name + ".min", range.min(), 0, limit)
                .range(name + ".max", range.max(), 0, limit)
                .that(range.min() <= range.max(), name, "Нижняя граница больше верхней");
    }
}
