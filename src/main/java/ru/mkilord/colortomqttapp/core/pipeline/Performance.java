package ru.mkilord.colortomqttapp.core.pipeline;

/**
 * @param fps       кадров в секунду за последнюю секунду
 * @param captureMs среднее время снимка экрана, мс
 * @param processMs среднее время расчета и отправки, мс
 * @param idle      экран давно не меняется, кадры снимаются реже
 */
public record Performance(double fps, double captureMs, double processMs, boolean idle) {
}
