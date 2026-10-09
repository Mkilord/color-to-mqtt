package ru.mkilord.colortomqttapp.application;

import ru.mkilord.colortomqttapp.core.pipeline.Performance;

import java.time.Instant;

/**
 * Состояние захвата для главной страницы.
 *
 * @param color       последний цвет экрана, #rrggbb
 * @param connected   есть ли связь с брокером; null, если захват остановлен
 * @param performance скорость захвата; null, если захват остановлен
 */
public record CaptureStatus(boolean running, String color, String broker, String topic, Boolean connected,
                            String lastPayload, Instant lastSentAt, String error, Instant errorAt,
                            Performance performance) {
}
