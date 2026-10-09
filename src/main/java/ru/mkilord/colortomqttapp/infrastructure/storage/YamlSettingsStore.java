package ru.mkilord.colortomqttapp.infrastructure.storage;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mkilord.colortomqttapp.application.SettingsStore;
import ru.mkilord.colortomqttapp.config.AppProperties;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Хранит настройки в YAML:
 * <ul>
 *     <li>{@code settings.yaml}: подключение к MQTT и имя активного профиля;</li>
 *     <li>{@code profiles/<имя>.yaml}: только отличия профиля от значений по умолчанию.</li>
 * </ul>
 * Благодаря хранению отличий новые значения по умолчанию из следующих версий доходят
 * до старых профилей сами.
 */
@Slf4j
@Component
public class YamlSettingsStore implements SettingsStore {

    private static final String SETTINGS_FILE = "settings.yaml";
    private static final String PROFILES_DIR = "profiles";
    private static final String EXTENSION = ".yaml";
    private static final Comparator<ProfileName> BY_NAME =
            Comparator.comparing(ProfileName::value, Collator.getInstance(Locale.forLanguageTag("ru")));

    private final Path root;
    private final MqttConnection defaultConnection;
    private final ObjectMapper yaml;

    @Autowired
    public YamlSettingsStore(AppProperties properties) {
        this(properties.storagePath(), properties.mqtt());
    }

    YamlSettingsStore(Path root, MqttConnection defaultConnection) {
        this.root = root;
        this.defaultConnection = defaultConnection;
        this.yaml = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public synchronized MqttConnection loadConnection() {
        var stored = settings().get("connection");
        if (stored == null) {
            return defaultConnection;
        }
        var merged = YamlFiles.merge(yaml.valueToTree(defaultConnection), stored);
        try {
            return yaml.treeToValue(merged, MqttConnection.class);
        } catch (IOException | IllegalArgumentException e) {
            log.error("Подключение в {} не прочитано, беру значения по умолчанию: {}", settingsFile(), e.getMessage());
            return defaultConnection;
        }
    }

    @Override
    public synchronized void saveConnection(MqttConnection connection) {
        var settings = settings();
        settings.set("connection", yaml.valueToTree(connection));
        YamlFiles.write(yaml, settingsFile(), settings);
    }

    @Override
    public synchronized Optional<ProfileName> loadActiveProfile() {
        var name = settings().path("activeProfile").asText("");
        return name.isEmpty() ? Optional.empty() : Optional.of(ProfileName.of(name));
    }

    @Override
    public synchronized void saveActiveProfile(ProfileName name) {
        var settings = settings();
        settings.put("activeProfile", name.value());
        YamlFiles.write(yaml, settingsFile(), settings);
    }

    @Override
    public synchronized List<ProfileName> profileNames() {
        var dir = profilesDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        var names = new ArrayList<ProfileName>();
        try (var files = Files.list(dir)) {
            files.map(path -> path.getFileName().toString())
                    .filter(file -> file.endsWith(EXTENSION))
                    .map(file -> file.substring(0, file.length() - EXTENSION.length()))
                    .forEach(file -> {
                        try {
                            names.add(ProfileName.of(file));
                        } catch (SettingsValidationException e) {
                            log.warn("Файл профиля с недопустимым именем пропущен: {}", file);
                        }
                    });
        } catch (IOException e) {
            throw new StorageException("Не удалось прочитать папку профилей " + dir, e);
        }
        names.sort(BY_NAME);
        return names;
    }

    @Override
    public synchronized Optional<ProfileSettings> loadProfile(ProfileName name) {
        var file = profileFile(name);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        var merged = YamlFiles.merge(defaults(), YamlFiles.read(yaml, file));
        try {
            return Optional.of(yaml.treeToValue(merged, ProfileSettings.class));
        } catch (IOException | IllegalArgumentException e) {
            throw new StorageException("Профиль «" + name + "» в " + file + " поврежден: " + rootMessage(e), e);
        }
    }

    @Override
    public synchronized void saveProfile(ProfileName name, ProfileSettings settings) {
        YamlFiles.write(yaml, profileFile(name), YamlFiles.diff(yaml.valueToTree(settings), defaults()));
    }

    @Override
    public synchronized void renameProfile(ProfileName from, ProfileName to) {
        try {
            Files.move(profileFile(from), profileFile(to));
        } catch (IOException e) {
            throw new StorageException("Не удалось переименовать профиль «" + from + "»", e);
        }
    }

    @Override
    public synchronized void deleteProfile(ProfileName name) {
        try {
            Files.deleteIfExists(profileFile(name));
        } catch (IOException e) {
            throw new StorageException("Не удалось удалить профиль «" + name + "»", e);
        }
    }

    private ObjectNode defaults() {
        return yaml.valueToTree(ProfileSettings.DEFAULTS);
    }

    private ObjectNode settings() {
        var file = settingsFile();
        if (!Files.exists(file)) {
            return yaml.createObjectNode();
        }
        JsonNode node = YamlFiles.read(yaml, file);
        return node.isObject() ? (ObjectNode) node : yaml.createObjectNode();
    }

    private Path settingsFile() {
        return root.resolve(SETTINGS_FILE);
    }

    private Path profilesDir() {
        return root.resolve(PROFILES_DIR);
    }

    private Path profileFile(ProfileName name) {
        return profilesDir().resolve(name.value() + EXTENSION);
    }

    private static String rootMessage(Throwable e) {
        var cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
