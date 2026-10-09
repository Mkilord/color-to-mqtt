package ru.mkilord.colortomqttapp.domain.settings;

public enum DetectionMethod {
    /** Самый весомый сектор тона среди цветных точек. */
    DOMINANT,
    /** Среднее с весом насыщенность x яркость. */
    VIVID,
    /** Простое среднее. */
    AVERAGE
}
