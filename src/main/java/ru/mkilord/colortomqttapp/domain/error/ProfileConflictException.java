package ru.mkilord.colortomqttapp.domain.error;

/**
 * Операция с профилем невозможна в текущем состоянии: имя занято, профиль последний и т. п.
 */
public class ProfileConflictException extends DomainException {

    public ProfileConflictException(String message) {
        super(message);
    }
}
