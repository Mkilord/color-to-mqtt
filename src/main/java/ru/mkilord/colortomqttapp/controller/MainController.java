package ru.mkilord.colortomqttapp.controller;

import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;

import static lombok.AccessLevel.PRIVATE;

@Log4j2
@Controller
@RequestMapping("/")
@AllArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MainController {
    ColorService colorService;
    SettingsService settingsService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("status", colorService.getStatus());
        model.addAttribute("profiles", settingsService.profiles());
        model.addAttribute("activeProfile", settingsService.activeProfile());
        return "index";
    }

    @PostMapping("/start")
    public ResponseEntity<String> startColorDetection() {
        try {
            colorService.start();
            return ResponseEntity.ok("Захват запущен");
        } catch (RuntimeException e) {
            log.error("Не удалось запустить захват цвета", e);
            return ResponseEntity.internalServerError().body("Не удалось запустить захват: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopColorDetection() {
        colorService.stop();
        return ResponseEntity.ok("Захват остановлен");
    }

    @GetMapping("/color")
    @ResponseBody
    public String getColor() {
        var color = colorService.getCurrentColor();
        return String.format("rgb(%d, %d, %d)", color.getRed(), color.getGreen(), color.getBlue());
    }
}
