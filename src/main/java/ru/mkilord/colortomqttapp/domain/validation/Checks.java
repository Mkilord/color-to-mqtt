package ru.mkilord.colortomqttapp.domain.validation;

import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.error.Violation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Собирает все нарушения одного объекта и бросает их разом, чтобы форма показала
 * ошибки у всех полей, а не только у первого.
 */
public final class Checks {

    private final String prefix;
    private final List<Violation> violations = new ArrayList<>();

    private Checks(String prefix) {
        this.prefix = prefix;
    }

    public static Checks of(String prefix) {
        return new Checks(prefix);
    }

    public Checks range(String field, double value, double min, double max) {
        if (Double.isNaN(value) || value < min || value > max) {
            fail(field, "От " + format(min) + " до " + format(max));
        }
        return this;
    }

    public Checks notNull(String field, Object value) {
        if (value == null) {
            fail(field, "Не задано");
        }
        return this;
    }

    public Checks that(boolean condition, String field, String message) {
        if (!condition) {
            fail(field, message);
        }
        return this;
    }

    public void validate() {
        if (!violations.isEmpty()) {
            throw new SettingsValidationException(violations);
        }
    }

    private void fail(String field, String message) {
        violations.add(new Violation(prefix.isEmpty() ? field : prefix + "." + field, message));
    }

    /**
     * Строит несколько независимых частей и сообщает о нарушениях во всех сразу.
     */
    public static final class Collector {

        private final List<Violation> violations = new ArrayList<>();

        public <T> T attempt(Supplier<T> builder) {
            try {
                return builder.get();
            } catch (SettingsValidationException e) {
                violations.addAll(e.violations());
                return null;
            }
        }

        public boolean ok() {
            return violations.isEmpty();
        }

        public void validate() {
            if (!violations.isEmpty()) {
                throw new SettingsValidationException(violations);
            }
        }
    }

    public static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
