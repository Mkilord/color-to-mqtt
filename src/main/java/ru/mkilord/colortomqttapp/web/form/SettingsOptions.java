package ru.mkilord.colortomqttapp.web.form;

import ru.mkilord.colortomqttapp.domain.settings.ComparisonMethod;
import ru.mkilord.colortomqttapp.domain.settings.DetectionMethod;
import ru.mkilord.colortomqttapp.domain.settings.SamplingPattern;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Подписи вариантов для выпадающих списков страницы настроек, в порядке показа.
 */
public final class SettingsOptions {

    public static final Map<DetectionMethod, String> DETECTION = ordered(
            entry(DetectionMethod.DOMINANT, "Преобладающий цвет"),
            entry(DetectionMethod.VIVID, "Среднее с упором на яркие цвета"),
            entry(DetectionMethod.AVERAGE, "Простое среднее"));

    public static final Map<SamplingPattern, String> SAMPLING = ordered(
            entry(SamplingPattern.CHESS, "Шахматный порядок"),
            entry(SamplingPattern.GRID, "Каждая клетка"));

    public static final Map<ComparisonMethod, String> COMPARISON = ordered(
            entry(ComparisonMethod.HSB_TOLERANCE, "По допускам H, S, B"),
            entry(ComparisonMethod.RGB_DISTANCE, "По расстоянию в RGB"),
            entry(ComparisonMethod.ANY_CHANGE, "Любое изменение"));

    private SettingsOptions() {
    }

    @SafeVarargs
    private static <K, V> Map<K, V> ordered(Map.Entry<K, V>... entries) {
        var map = new LinkedHashMap<K, V>();
        for (var e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }
}
