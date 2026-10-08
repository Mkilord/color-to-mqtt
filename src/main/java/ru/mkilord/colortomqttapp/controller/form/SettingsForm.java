package ru.mkilord.colortomqttapp.controller.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.mkilord.colortomqttapp.core.detector.AverageColorDetector;
import ru.mkilord.colortomqttapp.core.detector.ColorDetector;
import ru.mkilord.colortomqttapp.core.detector.DominantColorDetector;
import ru.mkilord.colortomqttapp.core.detector.VividColorDetector;
import ru.mkilord.colortomqttapp.core.processor.ChessProcessor;
import ru.mkilord.colortomqttapp.core.processor.GridProcessor;
import ru.mkilord.colortomqttapp.core.processor.Processor;
import ru.mkilord.colortomqttapp.core.tracker.DefaultColorStateTracker;
import ru.mkilord.colortomqttapp.core.tracker.SimpleColorStateTracker;
import ru.mkilord.colortomqttapp.core.tracker.StabilityGate;
import ru.mkilord.colortomqttapp.core.tracker.ToleranceColorStateTracker;
import ru.mkilord.colortomqttapp.core.zone.ColorZones;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Настройки, которые можно менять на странице /settings. Ключи свойств совпадают
 * с {@code app.defaultSettings} в application.yaml.
 */
@Data
public class SettingsForm {

    /**
     * Доступные трекеры изменения цвета: полное имя класса и подпись для интерфейса.
     */
    public static final Map<String, String> TRACKERS = new LinkedHashMap<>();

    static {
        TRACKERS.put(ToleranceColorStateTracker.class.getName(), "По допускам H, S, B");
        TRACKERS.put(DefaultColorStateTracker.class.getName(), "По расстоянию в RGB (чувствительность)");
        TRACKERS.put(SimpleColorStateTracker.class.getName(), "Любое изменение");
    }

    /**
     * Способы расчета цвета кадра.
     */
    public static final Map<String, String> DETECTORS = new LinkedHashMap<>();

    static {
        DETECTORS.put(DominantColorDetector.class.getName(), "Преобладающий цвет");
        DETECTORS.put(VividColorDetector.class.getName(), "Среднее с упором на яркие цвета");
        DETECTORS.put(AverageColorDetector.class.getName(), "Простое среднее");
    }

    /**
     * Порядок обхода точек кадра.
     */
    public static final Map<String, String> PROCESSORS = new LinkedHashMap<>();

    static {
        PROCESSORS.put(ChessProcessor.class.getName(), "Шахматный порядок");
        PROCESSORS.put(GridProcessor.class.getName(), "Каждая клетка");
    }

    @NotBlank(message = "Укажите адрес брокера")
    @Pattern(regexp = "^(tcp|ssl|ws|wss)://.+", message = "Адрес вида tcp://host:1883")
    private String broker;

    @Size(max = 255, message = "Логин не длиннее 255 символов")
    private String username;

    /**
     * Новый пароль. Пустое значение оставляет сохраненный пароль без изменений.
     */
    @Size(max = 255, message = "Пароль не длиннее 255 символов")
    private String password;

    /**
     * Сохранен ли пароль. Сам пароль на страницу не отдается.
     */
    private boolean passwordSet;

    @NotBlank(message = "Укажите топик")
    @Pattern(regexp = "^[^#+]+$", message = "Топик для публикации не может содержать # и +")
    private String topic;

    @NotNull(message = "Укажите период")
    @Min(value = 1, message = "От 1 мс")
    @Max(value = 10_000, message = "До 10000 мс")
    private Integer updatePeriod;

    @NotNull(message = "Укажите время")
    @Min(value = 0, message = "От 0 мс")
    @Max(value = 5_000, message = "До 5000 мс")
    private Integer holdTime;

    @NotNull(message = "Укажите ширину")
    @Min(value = 1, message = "От 1 px")
    @Max(value = 10_000, message = "До 10000 px")
    private Integer screenWidth;

    @NotNull(message = "Укажите высоту")
    @Min(value = 1, message = "От 1 px")
    @Max(value = 10_000, message = "До 10000 px")
    private Integer screenHeight;

    @NotNull(message = "Укажите шаг сетки")
    @Min(value = 1, message = "От 1 px")
    @Max(value = 1_000, message = "До 1000 px")
    private Integer cellSize;

    @NotBlank(message = "Выберите способ сравнения")
    private String stateTracker;

    @NotBlank(message = "Выберите способ расчета")
    private String detector;

    @NotBlank(message = "Выберите порядок обхода")
    private String processor;

    @NotNull(message = "Укажите долю")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "100", message = "До 100")
    private Float dominantMinShare;

    @NotNull(message = "Укажите чувствительность")
    @Min(value = 0, message = "От 0")
    @Max(value = 100, message = "До 100")
    private Integer sensitivity;

    @NotNull(message = "Укажите допуск")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "1", message = "До 1")
    private Float hueTolerance;

    @NotNull(message = "Укажите допуск")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "1", message = "До 1")
    private Float saturationTolerance;

    @NotNull(message = "Укажите допуск")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "1", message = "До 1")
    private Float brightnessTolerance;

    @NotNull(message = "Укажите сдвиг")
    @DecimalMin(value = "-360", message = "От -360")
    @DecimalMax(value = "360", message = "До 360")
    private Float modifyHue;

    @NotNull(message = "Укажите сдвиг")
    @DecimalMin(value = "-100", message = "От -100")
    @DecimalMax(value = "100", message = "До 100")
    private Float modifySaturation;

    @NotNull(message = "Укажите сдвиг")
    @DecimalMin(value = "-100", message = "От -100")
    @DecimalMax(value = "100", message = "До 100")
    private Float modifyBrightness;

    @NotNull(message = "Укажите усиление")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "100", message = "До 100")
    private Float saturationBoost;

    @NotNull(message = "Укажите порог")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "50", message = "До 50")
    private Float blackThreshold;

    @NotNull(message = "Укажите порог")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "100", message = "До 100")
    private Float grayThreshold;

    @NotNull(message = "Укажите минимум")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "360", message = "До 360")
    private Float minHue;

    @NotNull(message = "Укажите максимум")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "360", message = "До 360")
    private Float maxHue;

    @NotNull(message = "Укажите минимум")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "100", message = "До 100")
    private Float minSaturation;

    @NotNull(message = "Укажите максимум")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "100", message = "До 100")
    private Float maxSaturation;

    @NotNull(message = "Укажите минимум")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "100", message = "До 100")
    private Float minBrightness;

    @NotNull(message = "Укажите максимум")
    @DecimalMin(value = "0", message = "От 0")
    @DecimalMax(value = "100", message = "До 100")
    private Float maxBrightness;

    @AssertTrue(message = "Неизвестный способ расчета цвета")
    public boolean isDetectorKnown() {
        return detector == null || DETECTORS.containsKey(detector);
    }

    @AssertTrue(message = "Неизвестный порядок обхода")
    public boolean isProcessorKnown() {
        return processor == null || PROCESSORS.containsKey(processor);
    }

    @AssertTrue(message = "Неизвестный способ сравнения")
    public boolean isStateTrackerKnown() {
        return stateTracker == null || TRACKERS.containsKey(stateTracker);
    }

    @AssertTrue(message = "Минимум тона больше максимума")
    public boolean isHueRangeValid() {
        return minHue == null || maxHue == null || minHue <= maxHue;
    }

    @AssertTrue(message = "Минимум насыщенности больше максимума")
    public boolean isSaturationRangeValid() {
        return minSaturation == null || maxSaturation == null || minSaturation <= maxSaturation;
    }

    @AssertTrue(message = "Минимум яркости больше максимума")
    public boolean isBrightnessRangeValid() {
        return minBrightness == null || maxBrightness == null || minBrightness <= maxBrightness;
    }

    public static SettingsForm from(Properties p) {
        var form = new SettingsForm();
        form.setBroker(p.getProperty("broker"));
        form.setUsername(p.getProperty("username", ""));
        var savedPassword = p.getProperty("password");
        form.setPasswordSet(savedPassword != null && !savedPassword.isEmpty());
        form.setTopic(p.getProperty("topic"));
        form.setUpdatePeriod(toInt(p.getProperty("updatePeriod")));
        form.setHoldTime(toInt(p.getProperty(StabilityGate.HOLD_TIME_KEY, String.valueOf(StabilityGate.DEFAULT_HOLD_TIME))));
        form.setScreenWidth(toInt(p.getProperty("screenWight")));
        form.setScreenHeight(toInt(p.getProperty("screenHeight")));
        form.setCellSize(toInt(p.getProperty("cellSize")));
        form.setStateTracker(p.getProperty("stateTracker"));
        form.setDetector(p.getProperty(ColorDetector.DETECTOR_KEY));
        form.setProcessor(p.getProperty(Processor.PROCESSOR_KEY));
        form.setDominantMinShare(toFloat(p.getProperty(DominantColorDetector.MIN_SHARE_KEY,
                String.valueOf(DominantColorDetector.DEFAULT_MIN_SHARE))));
        form.setSensitivity(toInt(p.getProperty("sensitivity")));
        form.setHueTolerance(toFloat(p.getProperty("hueTolerance")));
        form.setSaturationTolerance(toFloat(p.getProperty("saturationTolerance")));
        form.setBrightnessTolerance(toFloat(p.getProperty("brightnessTolerance")));
        form.setModifyHue(toFloat(p.getProperty("modifyHue")));
        form.setModifySaturation(toFloat(p.getProperty("modifySaturation")));
        form.setModifyBrightness(toFloat(p.getProperty("modifyBrightness")));
        form.setSaturationBoost(toFloat(p.getProperty("saturationBoost", "0")));
        form.setBlackThreshold(toFloat(p.getProperty(ColorZones.BLACK_THRESHOLD_KEY,
                String.valueOf(ColorZones.DEFAULT_BLACK_THRESHOLD))));
        form.setGrayThreshold(toFloat(p.getProperty(ColorZones.GRAY_THRESHOLD_KEY,
                String.valueOf(ColorZones.DEFAULT_GRAY_THRESHOLD))));
        form.setMinHue(toFloat(p.getProperty("minHUE")));
        form.setMaxHue(toFloat(p.getProperty("maxHUE")));
        form.setMinSaturation(toFloat(p.getProperty("minSaturation")));
        form.setMaxSaturation(toFloat(p.getProperty("maxSaturation")));
        form.setMinBrightness(toFloat(p.getProperty("minBrightness")));
        form.setMaxBrightness(toFloat(p.getProperty("maxBrightness")));
        return form;
    }

    public void applyTo(Properties p) {
        p.setProperty("broker", broker.trim());
        var user = username == null ? "" : username.trim();
        p.setProperty("username", user);
        if (user.isEmpty()) {
            p.setProperty("password", "");
        } else if (password != null && !password.isEmpty()) {
            p.setProperty("password", password);
        }
        p.setProperty("topic", topic.trim());
        p.setProperty("updatePeriod", String.valueOf(updatePeriod));
        p.setProperty(StabilityGate.HOLD_TIME_KEY, String.valueOf(holdTime));
        p.setProperty("screenWight", String.valueOf(screenWidth));
        p.setProperty("screenHeight", String.valueOf(screenHeight));
        p.setProperty("cellSize", String.valueOf(cellSize));
        p.setProperty("stateTracker", stateTracker);
        p.setProperty(ColorDetector.DETECTOR_KEY, detector);
        p.setProperty(Processor.PROCESSOR_KEY, processor);
        p.setProperty(DominantColorDetector.MIN_SHARE_KEY, String.valueOf(dominantMinShare));
        p.setProperty("sensitivity", String.valueOf(sensitivity));
        p.setProperty("hueTolerance", String.valueOf(hueTolerance));
        p.setProperty("saturationTolerance", String.valueOf(saturationTolerance));
        p.setProperty("brightnessTolerance", String.valueOf(brightnessTolerance));
        p.setProperty("modifyHue", String.valueOf(modifyHue));
        p.setProperty("modifySaturation", String.valueOf(modifySaturation));
        p.setProperty("modifyBrightness", String.valueOf(modifyBrightness));
        p.setProperty("saturationBoost", String.valueOf(saturationBoost));
        p.setProperty(ColorZones.BLACK_THRESHOLD_KEY, String.valueOf(blackThreshold));
        p.setProperty(ColorZones.GRAY_THRESHOLD_KEY, String.valueOf(grayThreshold));
        p.setProperty("minHUE", String.valueOf(minHue));
        p.setProperty("maxHUE", String.valueOf(maxHue));
        p.setProperty("minSaturation", String.valueOf(minSaturation));
        p.setProperty("maxSaturation", String.valueOf(maxSaturation));
        p.setProperty("minBrightness", String.valueOf(minBrightness));
        p.setProperty("maxBrightness", String.valueOf(maxBrightness));
    }

    private static Integer toInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Float toFloat(String value) {
        try {
            return value == null ? null : Float.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
