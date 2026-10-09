package ru.mkilord.colortomqttapp.domain.error;

/**
 * Нарушение правил предметной области. Сообщение показывается пользователю как есть.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
