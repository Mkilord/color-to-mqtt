package ru.mkilord.colortomqttapp.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.mkilord.colortomqttapp.application.ConnectionService;
import ru.mkilord.colortomqttapp.application.ProfileService;
import ru.mkilord.colortomqttapp.domain.error.SettingsValidationException;
import ru.mkilord.colortomqttapp.domain.error.Violation;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.web.form.SettingsForm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Сохранение и сброс профиля со страницы настроек. Страница отправляет форму запросом fetch
 * и получает JSON; без JavaScript работает обычная отправка с перезагрузкой.
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    static final String FETCH_HEADER = "X-Requested-With";

    private final ProfileService profiles;
    private final ConnectionService connections;
    private final SettingsPageModel page;

    public SettingsController(ProfileService profiles, ConnectionService connections, SettingsPageModel page) {
        this.profiles = profiles;
        this.connections = connections;
        this.page = page;
    }

    @PostMapping
    public String save(@RequestParam(required = false) String profile,
                       @ModelAttribute("form") SettingsForm form, BindingResult binding,
                       Model model, RedirectAttributes redirect) {
        var name = page.resolve(profile);
        if (!binding.hasErrors()) {
            try {
                apply(name, form);
                redirect.addFlashAttribute("message", savedMessage(name));
                return "redirect:" + SettingsPageModel.url(name.value());
            } catch (SettingsValidationException e) {
                e.violations().forEach(v -> binding.rejectValue(SettingsForm.fieldOf(v.field()), "invalid", v.message()));
            }
        }
        page.fill(model, name);
        return "settings";
    }

    /**
     * Ошибки привязки (не число) возвращаются здесь, ошибки значений бросает домен,
     * их переводит в ответ {@link ApiExceptionHandler}.
     */
    @PostMapping(headers = FETCH_HEADER)
    public ResponseEntity<Map<String, Object>> saveFetch(@RequestParam(required = false) String profile,
                                                         @ModelAttribute("form") SettingsForm form,
                                                         BindingResult binding) {
        if (binding.hasErrors()) {
            var fields = new LinkedHashMap<String, String>();
            binding.getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), "Введите число"));
            return ResponseEntity.unprocessableEntity().body(Map.of("error", "Исправьте отмеченные поля", "errors", fields));
        }
        var name = page.resolve(profile);
        apply(name, form);
        return ResponseEntity.ok(Map.of("message", savedMessage(name),
                "passwordSet", !connections.current().password().isEmpty()));
    }

    @PostMapping("/reset")
    public String reset(@RequestParam(required = false) String profile, RedirectAttributes redirect) {
        var name = page.resolve(profile);
        profiles.reset(name);
        redirect.addFlashAttribute("message", "Профиль «" + name + "» сброшен к настройкам по умолчанию");
        return "redirect:" + SettingsPageModel.url(name.value());
    }

    /** Проверяет профиль и подключение вместе, чтобы показать все ошибки разом. */
    private void apply(ProfileName name, SettingsForm form) {
        var violations = new ArrayList<Violation>();
        var settings = attempt(form::toProfileSettings, violations);
        var connection = attempt(form::toConnection, violations);
        if (!violations.isEmpty()) {
            throw new SettingsValidationException(violations);
        }
        connections.update(connection);
        profiles.save(name, settings);
    }

    private static <T> T attempt(Supplier<T> build, List<Violation> violations) {
        try {
            return build.get();
        } catch (SettingsValidationException e) {
            violations.addAll(e.violations());
            return null;
        }
    }

    private static String savedMessage(ProfileName name) {
        return "Профиль «" + name + "» сохранен";
    }
}
