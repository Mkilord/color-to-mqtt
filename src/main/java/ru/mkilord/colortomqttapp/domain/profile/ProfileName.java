package ru.mkilord.colortomqttapp.domain.profile;

import ru.mkilord.colortomqttapp.domain.validation.Checks;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Имя профиля. Оно же имя файла, поэтому без путей и зарезервированных имен Windows.
 */
public record ProfileName(String value) {

    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N} _\\-]{0,39}");
    private static final Set<String> RESERVED = Set.of("con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    public static final ProfileName DEFAULT = new ProfileName("Основной");

    public ProfileName {
        value = value == null ? "" : value.strip();
        Checks.of("")
                .that(NAME.matcher(value).matches() && !RESERVED.contains(value.toLowerCase(Locale.ROOT)), "name",
                        "До 40 букв, цифр, пробелов, дефисов и подчеркиваний, начинается с буквы или цифры")
                .validate();
    }

    public static ProfileName of(String value) {
        return new ProfileName(value);
    }

    /** В Windows «Игры» и «игры» один файл. */
    public boolean sameFileAs(ProfileName other) {
        return value.equalsIgnoreCase(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
