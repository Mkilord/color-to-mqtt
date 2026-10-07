package ru.mkilord.colortomqttapp.service;

import java.awt.Color;

public interface ColorService {
    boolean isStart();

    void start();

    void stop();

    /**
     * Перезапускает захват с актуальными настройками, если он был запущен.
     */
    void restartIfRunning();

    Color getCurrentColor();

    ColorStatus getStatus();
}
