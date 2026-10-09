package ru.mkilord.colortomqttapp.domain.settings;

/**
 * Когда новый цвет считается отличным от отправленного.
 */
public enum ComparisonMethod {
    HSB_TOLERANCE,
    RGB_DISTANCE,
    ANY_CHANGE
}
