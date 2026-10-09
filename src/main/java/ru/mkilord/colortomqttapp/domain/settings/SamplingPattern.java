package ru.mkilord.colortomqttapp.domain.settings;

/**
 * По каким точкам области считается цвет.
 */
public enum SamplingPattern {
    /** Клетки сетки через одну, точек вдвое меньше. */
    CHESS,
    /** Каждая клетка сетки. */
    GRID
}
