package ru.mkilord.colortomqttapp.service;

import java.time.Instant;

/**
 * Состояние захвата для главной страницы.
 *
 * @param running      идет ли захват
 * @param color        последний определенный цвет экрана, #rrggbb
 * @param broker       адрес брокера из текущих настроек
 * @param topic        топик из текущих настроек
 * @param connected    есть ли соединение с брокером; null, если захват остановлен
 * @param lastPayload  последнее отправленное сообщение или null
 * @param lastSentAt   время последней отправки или null
 * @param error        описание текущей ошибки или null
 * @param errorAt      время ошибки или null
 * @param performance  скорость захвата или null, если захват остановлен
 */
public record ColorStatus(
        boolean running,
        String color,
        String broker,
        String topic,
        Boolean connected,
        String lastPayload,
        Instant lastSentAt,
        String error,
        Instant errorAt,
        Performance performance) {

    /**
     * @param fps       кадров в секунду за последнюю секунду
     * @param captureMs среднее время снимка экрана, мс
     * @param processMs среднее время расчета и отправки цвета, мс
     * @param idle      экран давно не менялся, захват идет с паузой в покое
     */
    public record Performance(double fps, double captureMs, double processMs, boolean idle) {
    }
}
