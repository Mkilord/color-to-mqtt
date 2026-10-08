package ru.mkilord.colortomqttapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mkilord.colortomqttapp.core.HSBColor;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.ColorStatus;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.util.Map;

/**
 * JSON для интерфейса: состояние захвата и размер экрана.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final ColorService colorService;

    @GetMapping("/status")
    public ColorStatus status() {
        return colorService.getStatus();
    }

    /**
     * Тело запроса для проверки цвета на лампах.
     */
    public record TestColor(float hue, float sat, float brightness) {
    }

    @PostMapping("/test-color")
    public ResponseEntity<Map<String, String>> testColor(@RequestBody TestColor color) {
        if (color.hue() < 0 || color.hue() > 360 || color.sat() < 0 || color.sat() > 100
                || color.brightness() < 0 || color.brightness() > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Тон 0..360, насыщенность и яркость 0..100"));
        }
        try {
            var payload = colorService.sendTestColor(new HSBColor(color.hue(), color.sat(), color.brightness()));
            return ResponseEntity.ok(Map.of("payload", payload));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Не удалось отправить: " + e.getMessage()));
        }
    }

    @GetMapping("/screen")
    public ResponseEntity<Map<String, Integer>> screen() {
        if (GraphicsEnvironment.isHeadless()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            var size = Toolkit.getDefaultToolkit().getScreenSize();
            return ResponseEntity.ok(Map.of("width", size.width, "height", size.height));
        } catch (HeadlessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
