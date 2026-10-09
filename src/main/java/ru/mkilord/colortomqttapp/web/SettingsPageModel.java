package ru.mkilord.colortomqttapp.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;
import ru.mkilord.colortomqttapp.application.ConnectionService;
import ru.mkilord.colortomqttapp.application.ProfileService;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;
import ru.mkilord.colortomqttapp.web.form.SettingsForm;
import ru.mkilord.colortomqttapp.web.form.SettingsOptions;

import java.util.Map;
import java.util.Set;

/**
 * Общие данные страницы настроек для показа и для повторного показа с ошибками.
 */
@Component
public class SettingsPageModel {

    private static final Set<String> CONNECTION_FIELDS = Set.of("broker", "topic", "username", "password", "passwordSet");

    private final ProfileService profiles;
    private final ConnectionService connections;
    private final ObjectMapper json;

    public SettingsPageModel(ProfileService profiles, ConnectionService connections, ObjectMapper json) {
        this.profiles = profiles;
        this.connections = connections;
        this.json = json;
    }

    /** Профиль из адреса страницы; неизвестное или недопустимое имя заменяется активным профилем. */
    public ProfileName resolve(String profile) {
        if (profile != null) {
            try {
                var name = ProfileName.of(profile);
                if (profiles.exists(name)) {
                    return name;
                }
            } catch (SettingsValidationException ignored) {
                // Имя из адреса пользователь мог исправить руками: показываем активный профиль.
            }
        }
        return profiles.active();
    }

    public void fill(Model model, ProfileName name) {
        model.addAttribute("profile", name.value());
        model.addAttribute("profiles", profiles.names().stream().map(ProfileName::value).toList());
        model.addAttribute("activeProfile", profiles.active().value());
        model.addAttribute("detectors", SettingsOptions.DETECTION);
        model.addAttribute("processors", SettingsOptions.SAMPLING);
        model.addAttribute("trackers", SettingsOptions.COMPARISON);
        model.addAttribute("defaultsJson", defaultsJson());
    }

    public static String url(String profile) {
        return UriComponentsBuilder.fromPath("/settings").queryParam("profile", profile).encode().toUriString();
    }

    /**
     * Значения по умолчанию: страница отмечает измененные поля и умеет вернуть их. Подключения тут нет.
     */
    private String defaultsJson() {
        Map<String, Object> defaults = json.convertValue(
                SettingsForm.of(ProfileSettings.DEFAULTS, connections.current()), new TypeReference<>() {
                });
        defaults.keySet().removeAll(CONNECTION_FIELDS);
        try {
            return json.writeValueAsString(defaults).replace("</", "<\\/");
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
