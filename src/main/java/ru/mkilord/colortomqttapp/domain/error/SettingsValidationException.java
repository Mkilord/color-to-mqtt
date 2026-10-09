package ru.mkilord.colortomqttapp.domain.error;

import java.util.List;
import java.util.stream.Collectors;

public class SettingsValidationException extends DomainException {

    private final List<Violation> violations;

    public SettingsValidationException(List<Violation> violations) {
        super(violations.stream().map(v -> v.field() + ": " + v.message()).collect(Collectors.joining("; ")));
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}
