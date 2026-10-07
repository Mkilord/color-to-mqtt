package ru.mkilord.colortomqttapp.controller;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mkilord.colortomqttapp.service.SettingsService;
import ru.mkilord.colortomqttapp.service.impl.ScreenRenderServiceImpl;

import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@Controller
@RequestMapping("/settings")
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SettingsController {
    Properties prop;
    SettingsService settingsService;

    @GetMapping("/preview_image")
    public ResponseEntity<byte[]> previewImage() {
        var screenRenderService = new ScreenRenderServiceImpl(prop);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(screenRenderService.getRenderedImage());
    }

    @GetMapping
    public String settingsPage(Model model) {
        model.addAttribute("mqttServer", prop.getProperty("broker"));
        model.addAttribute("interval", prop.getProperty("updatePeriod"));
        model.addAttribute("height", prop.getProperty("screenHeight"));
        model.addAttribute("width", prop.getProperty("screenWight"));
        return "settings";
    }

    /**
     * Сохраняет настройки в settings.txt. Превью сразу использует новый размер области,
     * захват цвета применит их при следующем запуске.
     */
    @PostMapping
    public String updateSettings(@RequestParam String mqttServer, @RequestParam int interval,
                                 @RequestParam int height, @RequestParam int width) {
        prop.setProperty("broker", mqttServer);
        prop.setProperty("updatePeriod", String.valueOf(interval));
        prop.setProperty("screenHeight", String.valueOf(height));
        prop.setProperty("screenWight", String.valueOf(width));
        settingsService.save(prop);
        return "redirect:/settings";
    }
}
