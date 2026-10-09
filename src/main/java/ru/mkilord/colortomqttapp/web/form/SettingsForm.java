package ru.mkilord.colortomqttapp.web.form;

import lombok.Data;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.error.Violation;
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
import ru.mkilord.colortomqttapp.domain.validation.Checks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Плоская форма страницы настроек. Проверки значений живут в доменных классах:
 * форма только переносит значения и переводит пути ошибок в имена своих полей.
 */
@Data
public class SettingsForm {

    /** Поле формы и путь к значению в доменной модели. */
    private static final Map<String, String> PATHS = new LinkedHashMap<>();

    static {
        PATHS.put("broker", "connection.broker");
        PATHS.put("topic", "connection.topic");
        PATHS.put("username", "connection.username");
        PATHS.put("password", "connection.password");
        PATHS.put("screenWidth", "capture.width");
        PATHS.put("screenHeight", "capture.height");
        PATHS.put("cellSize", "capture.cellSize");
        PATHS.put("processor", "capture.sampling");
        PATHS.put("updatePeriod", "capture.framePeriodMs");
        PATHS.put("idlePeriod", "capture.idlePeriodMs");
        PATHS.put("detector", "detection.method");
        PATHS.put("dominantMinShare", "detection.minSharePercent");
        PATHS.put("blackThreshold", "detection.blackThreshold");
        PATHS.put("grayThreshold", "detection.grayThreshold");
        PATHS.put("saturationBoost", "correction.saturationBoost");
        PATHS.put("modifyHue", "correction.hueShift");
        PATHS.put("modifySaturation", "correction.saturationShift");
        PATHS.put("modifyBrightness", "correction.brightnessShift");
        PATHS.put("hueShiftRed", "correction.hueShifts.red");
        PATHS.put("hueShiftYellow", "correction.hueShifts.yellow");
        PATHS.put("hueShiftGreen", "correction.hueShifts.green");
        PATHS.put("hueShiftCyan", "correction.hueShifts.cyan");
        PATHS.put("hueShiftBlue", "correction.hueShifts.blue");
        PATHS.put("hueShiftMagenta", "correction.hueShifts.magenta");
        PATHS.put("minHue", "correction.ranges.hue.min");
        PATHS.put("maxHue", "correction.ranges.hue.max");
        PATHS.put("minSaturation", "correction.ranges.saturation.min");
        PATHS.put("maxSaturation", "correction.ranges.saturation.max");
        PATHS.put("minBrightness", "correction.ranges.brightness.min");
        PATHS.put("maxBrightness", "correction.ranges.brightness.max");
        PATHS.put("holdTime", "sending.holdTimeMs");
        PATHS.put("stateTracker", "sending.comparison");
        PATHS.put("hueTolerance", "sending.hueTolerance");
        PATHS.put("saturationTolerance", "sending.saturationTolerance");
        PATHS.put("brightnessTolerance", "sending.brightnessTolerance");
        PATHS.put("sensitivity", "sending.rgbThresholdPercent");
    }

    private String broker;
    private String topic;
    private String username;
    /** Пустое поле оставляет сохраненный пароль. На страницу пароль не отдается. */
    private String password;
    private boolean passwordSet;

    private Integer screenWidth;
    private Integer screenHeight;
    private Integer cellSize;
    private SamplingPattern processor;
    private Integer updatePeriod;
    private Integer idlePeriod;

    private DetectionMethod detector;
    private Float dominantMinShare;
    private Float blackThreshold;
    private Float grayThreshold;

    private Float saturationBoost;
    private Float modifyHue;
    private Float modifySaturation;
    private Float modifyBrightness;
    private Float hueShiftRed;
    private Float hueShiftYellow;
    private Float hueShiftGreen;
    private Float hueShiftCyan;
    private Float hueShiftBlue;
    private Float hueShiftMagenta;
    private Float minHue;
    private Float maxHue;
    private Float minSaturation;
    private Float maxSaturation;
    private Float minBrightness;
    private Float maxBrightness;

    private Integer holdTime;
    private ComparisonMethod stateTracker;
    private Float hueTolerance;
    private Float saturationTolerance;
    private Float brightnessTolerance;
    private Integer sensitivity;

    public static SettingsForm of(ProfileSettings settings, MqttConnection connection) {
        var form = new SettingsForm();
        form.broker = connection.broker();
        form.topic = connection.topic();
        form.username = connection.username();
        form.passwordSet = !connection.password().isEmpty();

        var capture = settings.capture();
        form.screenWidth = capture.width();
        form.screenHeight = capture.height();
        form.cellSize = capture.cellSize();
        form.processor = capture.sampling();
        form.updatePeriod = capture.framePeriodMs();
        form.idlePeriod = capture.idlePeriodMs();

        var detection = settings.detection();
        form.detector = detection.method();
        form.dominantMinShare = detection.minSharePercent();
        form.blackThreshold = detection.blackThreshold();
        form.grayThreshold = detection.grayThreshold();

        var correction = settings.correction();
        form.saturationBoost = correction.saturationBoost();
        form.modifyHue = correction.hueShift();
        form.modifySaturation = correction.saturationShift();
        form.modifyBrightness = correction.brightnessShift();
        var shifts = correction.hueShifts();
        form.hueShiftRed = shifts.red();
        form.hueShiftYellow = shifts.yellow();
        form.hueShiftGreen = shifts.green();
        form.hueShiftCyan = shifts.cyan();
        form.hueShiftBlue = shifts.blue();
        form.hueShiftMagenta = shifts.magenta();
        var ranges = correction.ranges();
        form.minHue = ranges.hue().min();
        form.maxHue = ranges.hue().max();
        form.minSaturation = ranges.saturation().min();
        form.maxSaturation = ranges.saturation().max();
        form.minBrightness = ranges.brightness().min();
        form.maxBrightness = ranges.brightness().max();

        var sending = settings.sending();
        form.holdTime = sending.holdTimeMs();
        form.stateTracker = sending.comparison();
        form.hueTolerance = sending.hueTolerance();
        form.saturationTolerance = sending.saturationTolerance();
        form.brightnessTolerance = sending.brightnessTolerance();
        form.sensitivity = sending.rgbThresholdPercent();
        return form;
    }

    /**
     * @throws SettingsValidationException со всеми нарушениями сразу, пути доменные
     */
    public ProfileSettings toProfileSettings() {
        requireFilled();
        var parts = new Checks.Collector();
        var capture = parts.attempt(() -> CaptureSettings.builder()
                .width(screenWidth).height(screenHeight).cellSize(cellSize).sampling(processor)
                .framePeriodMs(updatePeriod).idlePeriodMs(idlePeriod)
                .build());
        var detection = parts.attempt(() -> DetectionSettings.builder()
                .method(detector).minSharePercent(dominantMinShare)
                .blackThreshold(blackThreshold).grayThreshold(grayThreshold)
                .build());
        var hueShifts = parts.attempt(() -> new HueShifts(hueShiftRed, hueShiftYellow, hueShiftGreen,
                hueShiftCyan, hueShiftBlue, hueShiftMagenta));
        var ranges = parts.attempt(() -> new ColorRanges(new ColorRange(minHue, maxHue),
                new ColorRange(minSaturation, maxSaturation), new ColorRange(minBrightness, maxBrightness)));
        var correction = parts.attempt(() -> CorrectionSettings.builder()
                .saturationBoost(saturationBoost).hueShift(modifyHue)
                .saturationShift(modifySaturation).brightnessShift(modifyBrightness)
                .hueShifts(hueShifts == null ? HueShifts.NONE : hueShifts)
                .ranges(ranges == null ? ColorRanges.FULL : ranges)
                .build());
        var sending = parts.attempt(() -> SendingSettings.builder()
                .holdTimeMs(holdTime).comparison(stateTracker)
                .hueTolerance(hueTolerance).saturationTolerance(saturationTolerance)
                .brightnessTolerance(brightnessTolerance).rgbThresholdPercent(sensitivity)
                .build());
        parts.validate();
        return new ProfileSettings(capture, detection, correction, sending);
    }

    public MqttConnection toConnection() {
        return new MqttConnection(broker, topic, username, password);
    }

    /** Имя поля формы для доменного пути ошибки; ошибка порядка границ показывается у верхней. */
    public static String fieldOf(String path) {
        for (var entry : PATHS.entrySet()) {
            if (entry.getValue().equals(path)) {
                return entry.getKey();
            }
        }
        return switch (path) {
            case "correction.ranges.hue" -> "maxHue";
            case "correction.ranges.saturation" -> "maxSaturation";
            case "correction.ranges.brightness" -> "maxBrightness";
            default -> path;
        };
    }

    private void requireFilled() {
        var values = new LinkedHashMap<String, Object>();
        values.put("screenWidth", screenWidth);
        values.put("screenHeight", screenHeight);
        values.put("cellSize", cellSize);
        values.put("processor", processor);
        values.put("updatePeriod", updatePeriod);
        values.put("idlePeriod", idlePeriod);
        values.put("detector", detector);
        values.put("dominantMinShare", dominantMinShare);
        values.put("blackThreshold", blackThreshold);
        values.put("grayThreshold", grayThreshold);
        values.put("saturationBoost", saturationBoost);
        values.put("modifyHue", modifyHue);
        values.put("modifySaturation", modifySaturation);
        values.put("modifyBrightness", modifyBrightness);
        values.put("hueShiftRed", hueShiftRed);
        values.put("hueShiftYellow", hueShiftYellow);
        values.put("hueShiftGreen", hueShiftGreen);
        values.put("hueShiftCyan", hueShiftCyan);
        values.put("hueShiftBlue", hueShiftBlue);
        values.put("hueShiftMagenta", hueShiftMagenta);
        values.put("minHue", minHue);
        values.put("maxHue", maxHue);
        values.put("minSaturation", minSaturation);
        values.put("maxSaturation", maxSaturation);
        values.put("minBrightness", minBrightness);
        values.put("maxBrightness", maxBrightness);
        values.put("holdTime", holdTime);
        values.put("stateTracker", stateTracker);
        values.put("hueTolerance", hueTolerance);
        values.put("saturationTolerance", saturationTolerance);
        values.put("brightnessTolerance", brightnessTolerance);
        values.put("sensitivity", sensitivity);
        List<Violation> missing = new ArrayList<>();
        values.forEach((field, value) -> {
            if (value == null) {
                missing.add(new Violation(PATHS.get(field), "Укажите значение"));
            }
        });
        if (!missing.isEmpty()) {
            throw new SettingsValidationException(missing);
        }
    }
}
