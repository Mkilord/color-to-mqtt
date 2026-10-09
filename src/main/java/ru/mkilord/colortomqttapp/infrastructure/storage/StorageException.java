package ru.mkilord.colortomqttapp.infrastructure.storage;

/**
 * Файл настроек не прочитан или не записан.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
