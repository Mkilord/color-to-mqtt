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
import ru.mkilord.colortomqttapp.service.impl.ScreenRenderServiceImpl;

import java.util.Properties;

import static lombok.AccessLevel.PRIVATE;

@Controller
@RequestMapping("/settings")
@FieldDefaults(level = PRIVATE, makeFinal = true)
@AllArgsConstructor
public class SettingsController {
    Properties prop;

    @GetMapping("/preview_image")
    public ResponseEntity<byte[]> previewImage() {
        var screenRenderService = new ScreenRenderServiceImpl(prop);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(screenRenderService.getRenderedImage());
    }

    @GetMapping
    public String settingsPage(Model model) {
        model.addAttribute("mqttServer", prop.get("text"));
        model.addAttribute("interval", prop.get("maxHSB"));
        return "settings";
    }

    @PostMapping
    public String updateSettings(@RequestParam String mqttServer, @RequestParam int interval) {
        return "redirect:/settings";
    }
}
