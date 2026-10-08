package ru.mkilord.colortomqttapp.service;

import ru.mkilord.colortomqttapp.core.HSBColor;

import java.awt.Color;

public interface ColorService {

    /** Пауза между кадрами, когда экран давно не меняется, мс; 0 отключает. */
    String IDLE_PERIOD_KEY = "idlePeriod";
    long DEFAULT_IDLE_PERIOD = 100;

    boolean isStart();

    void start();

    void stop();

    /**
     * Перезапускает захват с актуальными настройками, если он был запущен.
     */
    void restartIfRunning();

    Color getCurrentColor();

    ColorStatus getStatus();

    /**
     * Отправляет цвет на лампы как есть, без коррекции: для подбора поправок на глаз.
     * Если захват идет, следующее изменение экрана заменит этот цвет.
     *
     * @return отправленное сообщение
     */
    String sendTestColor(HSBColor color);
}
