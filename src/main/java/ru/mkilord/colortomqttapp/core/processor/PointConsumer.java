package ru.mkilord.colortomqttapp.core.processor;

/**
 * Принимает координаты точки без упаковки в Integer: при мелкой сетке точек десятки тысяч на кадр.
 */
@FunctionalInterface
public interface PointConsumer {
    void accept(int x, int y);
}
