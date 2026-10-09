package ru.mkilord.colortomqttapp.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mkilord.colortomqttapp.application.CaptureService;
import ru.mkilord.colortomqttapp.application.CaptureStatus;
import ru.mkilord.colortomqttapp.core.capture.ScreenCapture;
import ru.mkilord.colortomqttapp.domain.color.HsbColor;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CaptureController {

    private final CaptureService capture;
    private final ScreenCapture screen;

    public CaptureController(CaptureService capture, ScreenCapture screen) {
        this.capture = capture;
        this.screen = screen;
    }

    public record TestColorRequest(float hue, float sat, float brightness) {
    }

    @PostMapping("/capture/start")
    public Map<String, String> start() {
        capture.start();
        return Map.of("message", "Захват запущен");
    }

    @PostMapping("/capture/stop")
    public Map<String, String> stop() {
        capture.stop();
        return Map.of("message", "Захват остановлен");
    }

    @GetMapping("/status")
    public CaptureStatus status() {
        return capture.status();
    }

    @PostMapping("/test-color")
    public Map<String, String> testColor(@RequestBody TestColorRequest request) {
        var payload = capture.sendTestColor(new HsbColor(request.hue(), request.sat(), request.brightness()));
        return Map.of("payload", payload);
    }

    @GetMapping("/screen")
    public ResponseEntity<Map<String, Integer>> screen() {
        var size = screen.screenSize();
        if (size == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(Map.of("width", size.width, "height", size.height));
    }

    /** Снимок области для страницы настроек; размер передается из еще не сохраненной формы. */
    @GetMapping(value = "/snapshot", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] snapshot(@RequestParam int width, @RequestParam int height) {
        if (width < 1 || height < 1 || width > 10_000 || height > 10_000) {
            throw new IllegalArgumentException("Размер области от 1 до 10000 px");
        }
        return capture.snapshot(width, height);
    }
}
