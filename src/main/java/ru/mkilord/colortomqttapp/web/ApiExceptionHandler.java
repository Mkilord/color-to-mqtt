package ru.mkilord.colortomqttapp.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.mkilord.colortomqttapp.domain.error.ProfileConflictException;
import ru.mkilord.colortomqttapp.domain.error.ProfileNotFoundException;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.error.UnavailableException;
import ru.mkilord.colortomqttapp.infrastructure.storage.StorageException;
import ru.mkilord.colortomqttapp.web.form.SettingsForm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Единственное место, где исключения превращаются в HTTP-ответы. Тело всегда содержит
 * {@code error} с текстом для пользователя, ошибки проверки еще и {@code errors} по полям формы.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SettingsValidationException.class)
    public ResponseEntity<Map<String, Object>> invalid(SettingsValidationException e) {
        var fields = new LinkedHashMap<String, String>();
        e.violations().forEach(v -> fields.putIfAbsent(SettingsForm.fieldOf(v.field()), v.message()));
        var messages = e.violations().stream().map(v -> v.message()).distinct().toList();
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("error", String.join("; ", messages), "errors", fields));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(ProfileNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ProfileConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ProfileConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(UnavailableException.class)
    public ResponseEntity<Map<String, Object>> unavailable(UnavailableException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> storage(StorageException e) {
        log.error("Ошибка хранилища настроек", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message == null ? status.getReasonPhrase() : message));
    }
}
