package ru.mkilord.colortomqttapp.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.CaptureSettings;
import ru.mkilord.colortomqttapp.domain.settings.ColorRange;
import ru.mkilord.colortomqttapp.domain.settings.ColorRanges;
import ru.mkilord.colortomqttapp.domain.settings.ComparisonMethod;
import ru.mkilord.colortomqttapp.domain.settings.CorrectionSettings;
import ru.mkilord.colortomqttapp.domain.settings.DetectionMethod;
import ru.mkilord.colortomqttapp.domain.settings.DetectionSettings;
import ru.mkilord.colortomqttapp.domain.settings.HueShifts;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;
import ru.mkilord.colortomqttapp.domain.settings.SamplingPattern;
import ru.mkilord.colortomqttapp.domain.settings.SendingSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Переносит настройки версий до 1.1 в YAML один раз, если settings.yaml еще нет:
 * <ul>
 *     <li>{@code settings.txt} со всеми настройками или только с подключением и активным профилем;</li>
 *     <li>{@code profiles/*.properties}.</li>
 * </ul>
 * Старые файлы переименовываются в {@code .bak}. Значения вне допустимых границ заменяются
 * значениями по умолчанию для своего раздела.
 */
@Slf4j
final class LegacySettingsImporter {

    private static final String LEGACY_SETTINGS = "settings.txt";
    private static final String LEGACY_EXTENSION = ".properties";

    private final Path root;
    private final YamlSettingsStore store;

    LegacySettingsImporter(Path root, YamlSettingsStore store) {
        this.root = root;
        this.store = store;
    }

    void importIfNeeded() {
        if (store.exists()) {
            return;
        }
        var legacySettings = root.resolve(LEGACY_SETTINGS);
        var global = read(legacySettings);
        store.saveConnection(connection(global));

        var profiles = legacyProfiles();
        if (profiles.isEmpty()) {
            profiles.put(ProfileName.DEFAULT, global);
        }
        profiles.forEach((name, values) -> store.saveProfile(name, profile(name, values)));

        var active = global.getProperty("activeProfile");
        var activeName = profiles.keySet().stream()
                .filter(name -> name.value().equals(active))
                .findFirst()
                .orElse(profiles.keySet().iterator().next());
        store.saveActiveProfile(activeName);

        backup(legacySettings);
        profiles.keySet().forEach(name -> backup(root.resolve(YamlSettingsStore.PROFILES_DIR).resolve(name.value() + LEGACY_EXTENSION)));
        log.info("Настройки перенесены в {}", root.resolve(YamlSettingsStore.SETTINGS_FILE));
    }

    private Map<ProfileName, Properties> legacyProfiles() {
        var result = new LinkedHashMap<ProfileName, Properties>();
        var dir = root.resolve(YamlSettingsStore.PROFILES_DIR);
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (var files = Files.list(dir)) {
            files.filter(file -> file.getFileName().toString().endsWith(LEGACY_EXTENSION)).sorted().forEach(file -> {
                var fileName = file.getFileName().toString();
                try {
                    result.put(ProfileName.of(fileName.substring(0, fileName.length() - LEGACY_EXTENSION.length())), read(file));
                } catch (SettingsValidationException e) {
                    log.warn("Старый профиль с недопустимым именем пропущен: {}", fileName);
                }
            });
        } catch (IOException e) {
            throw new StorageException("Не удалось прочитать папку профилей " + dir, e);
        }
        return result;
    }

    MqttConnection connection(Properties p) {
        var defaults = store.loadConnection();
        return orDefault("подключение", () -> new MqttConnection(
                p.getProperty("broker", defaults.broker()),
                p.getProperty("topic", defaults.topic()),
                p.getProperty("username", defaults.username()),
                p.getProperty("password", defaults.password())), defaults);
    }

    static ProfileSettings profile(ProfileName name, Properties p) {
        var values = new Values(p);
        var capture = orDefault(name + ": захват", () -> CaptureSettings.builder()
                .width(values.integer("screenWight", CaptureSettings.DEFAULTS.width()))
                .height(values.integer("screenHeight", CaptureSettings.DEFAULTS.height()))
                .cellSize(values.integer("cellSize", CaptureSettings.DEFAULTS.cellSize()))
                .sampling(p.getProperty("processor", "").endsWith("GridProcessor") ? SamplingPattern.GRID : SamplingPattern.CHESS)
                .framePeriodMs(values.integer("updatePeriod", CaptureSettings.DEFAULTS.framePeriodMs()))
                .idlePeriodMs(values.integer("idlePeriod", CaptureSettings.DEFAULTS.idlePeriodMs()))
                .build(), CaptureSettings.DEFAULTS);
        var detection = orDefault(name + ": распознавание", () -> DetectionSettings.builder()
                .method(detectionMethod(p.getProperty("detector", "")))
                .minSharePercent(values.decimal("dominantMinShare", DetectionSettings.DEFAULTS.minSharePercent()))
                .blackThreshold(values.decimal("blackThreshold", DetectionSettings.DEFAULTS.blackThreshold()))
                .grayThreshold(values.decimal("grayThreshold", DetectionSettings.DEFAULTS.grayThreshold()))
                .build(), DetectionSettings.DEFAULTS);
        var correction = orDefault(name + ": коррекция", () -> CorrectionSettings.builder()
                .saturationBoost(values.decimal("saturationBoost", CorrectionSettings.DEFAULTS.saturationBoost()))
                .hueShift(values.decimal("modifyHue", 0))
                .saturationShift(values.decimal("modifySaturation", 0))
                .brightnessShift(values.decimal("modifyBrightness", 0))
                .hueShifts(new HueShifts(values.decimal("hueShiftRed", 0), values.decimal("hueShiftYellow", 0),
                        values.decimal("hueShiftGreen", 0), values.decimal("hueShiftCyan", 0),
                        values.decimal("hueShiftBlue", 0), values.decimal("hueShiftMagenta", 0)))
                .ranges(new ColorRanges(
                        new ColorRange(values.decimal("minHUE", 0), values.decimal("maxHUE", 360)),
                        new ColorRange(values.decimal("minSaturation", 0), values.decimal("maxSaturation", 100)),
                        new ColorRange(values.decimal("minBrightness", 0), values.decimal("maxBrightness", 100))))
                .build(), CorrectionSettings.DEFAULTS);
        var sending = orDefault(name + ": отправка", () -> SendingSettings.builder()
                .holdTimeMs(values.integer("holdTime", SendingSettings.DEFAULTS.holdTimeMs()))
                .comparison(comparisonMethod(p.getProperty("stateTracker", "")))
                .hueTolerance(values.decimal("hueTolerance", SendingSettings.DEFAULTS.hueTolerance()))
                .saturationTolerance(values.decimal("saturationTolerance", SendingSettings.DEFAULTS.saturationTolerance()))
                .brightnessTolerance(values.decimal("brightnessTolerance", SendingSettings.DEFAULTS.brightnessTolerance()))
                .rgbThresholdPercent(values.integer("sensitivity", SendingSettings.DEFAULTS.rgbThresholdPercent()))
                .build(), SendingSettings.DEFAULTS);
        return new ProfileSettings(capture, detection, correction, sending);
    }

    private static DetectionMethod detectionMethod(String className) {
        if (className.endsWith("AverageColorDetector")) {
            return DetectionMethod.AVERAGE;
        }
        return className.endsWith("VividColorDetector") ? DetectionMethod.VIVID : DetectionMethod.DOMINANT;
    }

    private static ComparisonMethod comparisonMethod(String className) {
        if (className.endsWith("DefaultColorStateTracker")) {
            return ComparisonMethod.RGB_DISTANCE;
        }
        return className.endsWith("SimpleColorStateTracker") ? ComparisonMethod.ANY_CHANGE : ComparisonMethod.HSB_TOLERANCE;
    }

    private static <T> T orDefault(String section, Supplier<T> build, T fallback) {
        try {
            return build.get();
        } catch (SettingsValidationException e) {
            log.warn("Старые настройки ({}) вне допустимых границ, беру значения по умолчанию: {}", section, e.getMessage());
            return fallback;
        }
    }

    private static Properties read(Path file) {
        var properties = new Properties();
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new StorageException("Не удалось прочитать " + file, e);
            }
        }
        return properties;
    }

    private static void backup(Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".bak"));
        } catch (IOException e) {
            log.warn("Не удалось переименовать {} в .bak: {}", file, e.getMessage());
        }
    }

    private record Values(Properties properties) {

        int integer(String key, int fallback) {
            var value = properties.getProperty(key);
            try {
                return value == null ? fallback : (int) Math.round(Double.parseDouble(value.strip()));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        float decimal(String key, float fallback) {
            var value = properties.getProperty(key);
            try {
                return value == null ? fallback : Float.parseFloat(value.strip());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
