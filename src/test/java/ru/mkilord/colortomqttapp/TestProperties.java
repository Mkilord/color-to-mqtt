package ru.mkilord.colortomqttapp;

import java.util.Properties;

/**
 * Настройки для тестов, совпадающие с app.defaultSettings из application.yaml.
 */
public final class TestProperties {

    private TestProperties() {
    }

    public static Properties defaults() {
        var p = new Properties();
        p.setProperty("screenHeight", "400");
        p.setProperty("screenWight", "400");
        p.setProperty("updatePeriod", "10");
        p.setProperty("processor", "ru.mkilord.colortomqttapp.core.processor.ChessProcessor");
        p.setProperty("detector", "ru.mkilord.colortomqttapp.core.detector.AverageColorDetector");
        p.setProperty("screenAreaType", "ru.mkilord.colortomqttapp.core.screenshoter.screenArea.DefaultScreenArea");
        p.setProperty("stateTracker", "ru.mkilord.colortomqttapp.core.tracker.ToleranceColorStateTracker");
        p.setProperty("sensitivity", "30");
        p.setProperty("hueTolerance", "0.2");
        p.setProperty("saturationTolerance", "0.3");
        p.setProperty("brightnessTolerance", "0.3");
        p.setProperty("modifyHue", "2");
        p.setProperty("modifySaturation", "10");
        p.setProperty("modifyBrightness", "2");
        p.setProperty("cellSize", "20");
        p.setProperty("broker", "tcp://localhost:1883");
        p.setProperty("topic", "colorToMQTT");
        p.setProperty("username", "");
        p.setProperty("password", "");
        p.setProperty("minHUE", "0");
        p.setProperty("maxHUE", "360");
        p.setProperty("minSaturation", "10");
        p.setProperty("maxSaturation", "100");
        p.setProperty("minBrightness", "0");
        p.setProperty("maxBrightness", "100");
        return p;
    }
}
