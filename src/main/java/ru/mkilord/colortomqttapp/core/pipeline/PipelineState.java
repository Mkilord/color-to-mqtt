package ru.mkilord.colortomqttapp.core.pipeline;

import java.awt.Color;
import java.time.Instant;

/**
 * Снимок состояния идущего захвата.
 *
 * @param color       последний принятый цвет экрана
 * @param lastPayload последнее отправленное сообщение или null
 * @param error       ошибка захвата или соединения, если есть
 */
public record PipelineState(Color color, boolean connected, String lastPayload, Instant lastSentAt,
                            String error, Instant errorAt, Performance performance) {
}
