package ru.mkilord.colortomqttapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
