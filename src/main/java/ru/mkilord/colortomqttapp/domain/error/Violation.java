package ru.mkilord.colortomqttapp.domain.error;

/**
 * @param field   путь к значению, например {@code capture.width}
 * @param message что не так, для пользователя
 */
public record Violation(String field, String message) {
}
