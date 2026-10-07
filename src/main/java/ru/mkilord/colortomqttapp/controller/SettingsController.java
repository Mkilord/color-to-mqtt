package ru.mkilord.colortomqttapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.mkilord.colortomqttapp.controller.form.SettingsForm;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;
import ru.mkilord.colortomqttapp.service.impl.ScreenRenderServiceImpl;

import java.util.Map;
import java.util.Properties;

/**
 * Страница настроек. Сохраненные настройки пишутся в settings.txt и сразу применяются:
 * если захват запущен, он перезапускается с новыми параметрами.
 */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final Properties prop;
    private final SettingsService settingsService;
    private final ColorService colorService;

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
        var settings = new Properties();
        settings.putAll(prop);
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
    public String settingsPage(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", SettingsForm.from(prop));
        }
        return "settings";
    }

    @PostMapping
    public String updateSettings(@Valid @ModelAttribute("form") SettingsForm form, BindingResult result,
                                 RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "settings";
        }
        form.applyTo(prop);
        settingsService.save(prop);
        colorService.restartIfRunning();
        redirect.addFlashAttribute("message", "Настройки сохранены");
        return "redirect:/settings";
    }

    @PostMapping("/reset")
    public String resetSettings(RedirectAttributes redirect) {
        var defaults = settingsService.resetToDefaults();
        prop.clear();
        prop.putAll(defaults);
        colorService.restartIfRunning();
        redirect.addFlashAttribute("message", "Восстановлены настройки по умолчанию");
        return "redirect:/settings";
    }
}
