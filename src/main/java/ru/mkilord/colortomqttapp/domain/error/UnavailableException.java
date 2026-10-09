package ru.mkilord.colortomqttapp.domain.error;

/**
 * Нужный ресурс сейчас недоступен: нет графической среды для захвата, нет связи с брокером.
 */
public class UnavailableException extends DomainException {

    public UnavailableException(String message) {
        super(message);
    }
}
