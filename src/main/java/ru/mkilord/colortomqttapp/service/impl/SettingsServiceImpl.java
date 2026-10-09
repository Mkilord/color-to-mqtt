package ru.mkilord.colortomqttapp.service.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import ru.mkilord.colortomqttapp.config.SettingsConfig;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Хранит общие настройки в settings.txt, а профили в папке profiles, по файлу на профиль.
 * <p>
 * В settings.txt лежат подключение к брокеру и имя активного профиля. Файл профиля хранит
 * все остальные ключи. При первом запуске старый settings.txt со всеми настройками
 * разделяется: общее остается в нем, остальное переносится в профиль «Основной».
 */
@Log4j2
@Component
public class SettingsServiceImpl implements SettingsService {

    static final String ACTIVE_PROFILE_KEY = "activeProfile";
    private static final String EXTENSION = ".properties";
    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N} _\\-]{0,39}");
    private static final Set<String> RESERVED = Set.of("con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    private final SettingsConfig config;

    public SettingsServiceImpl(SettingsConfig config) {
        this.config = config;
    }

    @Override
    public synchronized List<String> profiles() {
        ensureInitialized();
        return listProfiles();
    }

    @Override
    public synchronized String activeProfile() {
        ensureInitialized();
        var active = readGlobal().getProperty(ACTIVE_PROFILE_KEY);
        var all = listProfiles();
        return all.contains(active) ? active : all.get(0);
    }

    @Override
    public synchronized Properties loadOrElseLoadDefault() {
        return loadProfile(activeProfile());
    }

    @Override
    public synchronized Properties loadProfile(String name) {
        ensureInitialized();
        requireExisting(name);
        var result = loadDefault();
        read(profileFile(name)).forEach((key, value) -> {
            if (!GLOBAL_KEYS.contains(key)) {
                result.put(key, value);
            }
        });
        var global = readGlobal();
        for (var key : GLOBAL_KEYS) {
            if (global.containsKey(key)) {
                result.setProperty(key, global.getProperty(key));
            }
        }
        return result;
    }

    @Override
    public Properties loadDefault() {
        var properties = new Properties();
        properties.putAll(config.getDefaultSettings());
        return properties;
    }

    @Override
    public synchronized void saveProfile(String name, Properties settings) {
        ensureInitialized();
        requireExisting(name);
        var global = readGlobal();
        var profile = new Properties();
        settings.forEach((key, value) -> {
            if (GLOBAL_KEYS.contains(key)) {
                global.put(key, value);
            } else if (!ACTIVE_PROFILE_KEY.equals(key)) {
                profile.put(key, value);
            }
        });
        write(config.getSettingsFilePath(), global, "Общие настройки ColorToMQTT");
        write(profileFile(name), profile, "Профиль " + name);
    }

    @Override
    public synchronized void activate(String name) {
        ensureInitialized();
        requireExisting(name);
        var global = readGlobal();
        global.setProperty(ACTIVE_PROFILE_KEY, name);
        write(config.getSettingsFilePath(), global, "Общие настройки ColorToMQTT");
    }

    @Override
    public synchronized void createProfile(String name, String copyFrom) {
        ensureInitialized();
        var clean = validName(name);
        requireFree(clean, null);
        var source = new Properties();
        if (copyFrom != null) {
            requireExisting(copyFrom);
            source = read(profileFile(copyFrom));
        }
        write(profileFile(clean), source, "Профиль " + clean);
    }

    @Override
    public synchronized void renameProfile(String from, String to) {
        ensureInitialized();
        requireExisting(from);
        var clean = validName(to);
        if (clean.equals(from)) {
            return;
        }
        requireFree(clean, from);
        var wasActive = from.equals(activeProfile());
        try {
            Files.move(profileFile(from), profileFile(clean));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось переименовать профиль", e);
        }
        if (wasActive) {
            activate(clean);
        }
    }

    @Override
    public synchronized void deleteProfile(String name) {
        ensureInitialized();
        requireExisting(name);
        if (listProfiles().size() == 1) {
            throw new IllegalArgumentException("Это последний профиль, его нельзя удалить");
        }
        var wasActive = name.equals(activeProfile());
        try {
            Files.delete(profileFile(name));
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось удалить профиль", e);
        }
        if (wasActive) {
            activate(listProfiles().get(0));
        }
    }

    @Override
    public synchronized Properties resetProfile(String name) {
        ensureInitialized();
        requireExisting(name);
        write(profileFile(name), new Properties(), "Профиль " + name);
        return loadProfile(name);
    }

    // ---- Файлы ----

    private Path profilesDir() {
        return config.getProfilesDirPath();
    }

    private Path profileFile(String name) {
        return profilesDir().resolve(name + EXTENSION);
    }

    private List<String> listProfiles() {
        var names = new ArrayList<String>();
        try (var files = Files.list(profilesDir())) {
            files.map(path -> path.getFileName().toString())
                    .filter(file -> file.endsWith(EXTENSION))
                    .map(file -> file.substring(0, file.length() - EXTENSION.length()))
                    .forEach(names::add);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать папку профилей " + profilesDir(), e);
        }
        names.sort(Collator.getInstance(Locale.forLanguageTag("ru")));
        return names;
    }

    /**
     * Создает папку профилей и при необходимости переносит старые настройки в профиль «Основной».
     */
    private void ensureInitialized() {
        try {
            Files.createDirectories(profilesDir());
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось создать папку профилей " + profilesDir(), e);
        }
        if (!listProfiles().isEmpty()) {
            return;
        }
        var old = readGlobal();
        var global = new Properties();
        var profile = new Properties();
        old.forEach((key, value) -> {
            if (GLOBAL_KEYS.contains(key)) {
                global.put(key, value);
            } else if (!ACTIVE_PROFILE_KEY.equals(key)) {
                profile.put(key, value);
            }
        });
        global.setProperty(ACTIVE_PROFILE_KEY, DEFAULT_PROFILE);
        write(profileFile(DEFAULT_PROFILE), profile, "Профиль " + DEFAULT_PROFILE);
        write(config.getSettingsFilePath(), global, "Общие настройки ColorToMQTT");
        log.info("Создан профиль «{}» из {}", DEFAULT_PROFILE, config.getSettingsFilePath());
    }

    private Properties readGlobal() {
        return read(config.getSettingsFilePath());
    }

    private static Properties read(Path file) {
        var properties = new Properties();
        if (!Files.exists(file)) {
            return properties;
        }
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            log.warn("Не удалось прочитать {}: {}", file, e.getMessage());
        }
        return properties;
    }

    private static void write(Path file, Properties properties, String comment) {
        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            properties.store(writer, comment);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить " + file, e);
        }
    }

    // ---- Проверки ----

    private void requireExisting(String name) {
        if (name == null || !listProfiles().contains(name)) {
            throw new IllegalArgumentException("Профиль «" + name + "» не найден");
        }
    }

    /**
     * Имена сравниваются без учета регистра: в Windows «Игры» и «игры» один файл.
     */
    private void requireFree(String name, String except) {
        for (var existing : listProfiles()) {
            if (!existing.equals(except) && existing.equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("Профиль «" + existing + "» уже есть");
            }
        }
    }

    static String validName(String name) {
        var clean = name == null ? "" : name.strip();
        if (!NAME.matcher(clean).matches() || RESERVED.contains(clean.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Имя профиля: до 40 букв, цифр, пробелов, дефисов и подчеркиваний, начинается с буквы или цифры");
        }
        return clean;
    }
}
