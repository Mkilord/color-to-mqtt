package ru.mkilord.colortomqttapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import ru.mkilord.colortomqttapp.controller.form.SettingsForm;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;
import ru.mkilord.colortomqttapp.service.impl.ScreenRenderServiceImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Страница настроек профиля. Сохранение сразу применяется: если захват запущен,
 * он перезапускается. Из страницы форма отправляется запросом fetch и получает JSON,
 * без JavaScript работает обычная отправка формы с перезагрузкой.
 */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    static final String FETCH_HEADER = "X-Requested-With";

    private final SettingsService settingsService;
    private final ColorService colorService;
    private final ObjectMapper objectMapper;

    @ModelAttribute("detectors")
    public Map<String, String> detectors() {
        return SettingsForm.DETECTORS;
    }

    @ModelAttribute("processors")
    public Map<String, String> processors() {
        return SettingsForm.PROCESSORS;
    }

    @ModelAttribute("trackers")
    public Map<String, String> trackers() {
        return SettingsForm.TRACKERS;
    }

    /**
     * Снимок области захвата. Размер можно передать параметрами, чтобы показать
     * еще не сохраненные значения из формы.
     */
    @GetMapping("/preview_image")
    public ResponseEntity<byte[]> previewImage(@RequestParam(required = false) Integer width,
                                               @RequestParam(required = false) Integer height) {
        var settings = settingsService.loadOrElseLoadDefault();
        if (width != null && width > 0 && width <= 10_000) {
            settings.setProperty("screenWight", String.valueOf(width));
        }
        if (height != null && height > 0 && height <= 10_000) {
            settings.setProperty("screenHeight", String.valueOf(height));
        }
        var screenRenderService = new ScreenRenderServiceImpl(settings);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(screenRenderService.getRenderedImage());
    }

    @GetMapping
    public String settingsPage(@RequestParam(required = false) String profile, Model model) {
        var name = resolve(profile);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", SettingsForm.from(settingsService.loadProfile(name)));
        }
        addProfiles(model, name);
        return "settings";
    }

    @PostMapping
    public String updateSettings(@RequestParam(required = false) String profile,
                                 @Valid @ModelAttribute("form") SettingsForm form, BindingResult result,
                                 Model model, RedirectAttributes redirect) {
        var name = resolve(profile);
        if (result.hasErrors()) {
            addProfiles(model, name);
            return "settings";
        }
        save(name, form);
        redirect.addFlashAttribute("message", savedMessage(name));
        return "redirect:" + pageUrl(name);
    }

    /**
     * То же сохранение для страницы: ответ JSON, страница не перезагружается и не прокручивается.
     */
    @PostMapping(headers = FETCH_HEADER)
    public ResponseEntity<Map<String, Object>> updateSettingsFetch(@RequestParam(required = false) String profile,
                                                                   @Valid @ModelAttribute("form") SettingsForm form,
                                                                   BindingResult result) {
        var name = resolve(profile);
        if (result.hasErrors()) {
            return ResponseEntity.unprocessableEntity().body(errors(result));
        }
        save(name, form);
        var saved = SettingsForm.from(settingsService.loadProfile(name));
        return ResponseEntity.ok(Map.of("message", savedMessage(name), "passwordSet", saved.isPasswordSet()));
    }

    private void save(String name, SettingsForm form) {
        var settings = settingsService.loadProfile(name);
        form.applyTo(settings);
        settingsService.saveProfile(name, settings);
        colorService.restartIfRunning();
    }

    private static String savedMessage(String name) {
        return "Профиль «" + name + "» сохранен";
    }

    @PostMapping("/reset")
    public String resetSettings(@RequestParam(required = false) String profile, RedirectAttributes redirect) {
        var name = resolve(profile);
        settingsService.resetProfile(name);
        colorService.restartIfRunning();
        redirect.addFlashAttribute("message", "Профиль «" + name + "» сброшен к настройкам по умолчанию");
        return "redirect:" + pageUrl(name);
    }

    private String resolve(String profile) {
        return profile != null && settingsService.profiles().contains(profile)
                ? profile
                : settingsService.activeProfile();
    }

    private void addProfiles(Model model, String name) {
        model.addAttribute("defaultsJson", defaultsJson());
        model.addAttribute("profile", name);
        model.addAttribute("profiles", settingsService.profiles());
        model.addAttribute("activeProfile", settingsService.activeProfile());
    }

    /**
     * Значения по умолчанию для страницы: она отмечает измененные расширенные настройки
     * и умеет вернуть их к умолчанию. Пароль не передается.
     */
    String defaultsJson() {
        Map<String, Object> defaults = objectMapper.convertValue(
                SettingsForm.from(settingsService.loadDefault()), new TypeReference<>() {
                });
        defaults.keySet().removeIf(key -> key.startsWith("password") || key.equals("username")
                || key.equals("broker") || key.equals("topic"));
        try {
            return objectMapper.writeValueAsString(defaults).replace("</", "<\\/");
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    static String pageUrl(String profile) {
        return UriComponentsBuilder.fromPath("/settings").queryParam("profile", profile).encode().toUriString();
    }

    /**
     * Ошибки проверки: по полю формы (для проверок диапазонов это имя свойства, например
     * hueRangeValid) и общий список сообщений.
     */
    static Map<String, Object> errors(BindingResult result) {
        var fields = new LinkedHashMap<String, String>();
        var messages = new ArrayList<String>();
        for (var error : result.getAllErrors()) {
            messages.add(error.getDefaultMessage());
            if (error instanceof FieldError field) {
                fields.putIfAbsent(field.getField(), field.getDefaultMessage());
            }
        }
        return Map.of("errors", fields, "messages", messages, "status", HttpStatus.UNPROCESSABLE_ENTITY.value());
    }
}
