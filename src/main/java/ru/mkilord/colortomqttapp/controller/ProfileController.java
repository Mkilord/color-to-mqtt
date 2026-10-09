package ru.mkilord.colortomqttapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.util.List;
import java.util.Map;

/**
 * Профили настроек: список, выбор активного, создание, переименование и удаление.
 * Смена активного профиля сразу применяется к идущему захвату.
 */
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final SettingsService settingsService;
    private final ColorService colorService;

    public record Profiles(String active, List<String> profiles) {
    }

    public record NameRequest(String name) {
    }

    public record CreateRequest(String name, String copyFrom) {
    }

    public record RenameRequest(String from, String to) {
    }

    @GetMapping
    public Profiles list() {
        return new Profiles(settingsService.activeProfile(), settingsService.profiles());
    }

    @PostMapping("/activate")
    public Profiles activate(@RequestBody NameRequest request) {
        if (!request.name().equals(settingsService.activeProfile())) {
            settingsService.activate(request.name());
            colorService.restartIfRunning();
        }
        return list();
    }

    @PostMapping
    public Profiles create(@RequestBody CreateRequest request) {
        settingsService.createProfile(request.name(), request.copyFrom());
        return list();
    }

    @PostMapping("/rename")
    public Profiles rename(@RequestBody RenameRequest request) {
        settingsService.renameProfile(request.from(), request.to());
        return list();
    }

    @PostMapping("/delete")
    public Profiles delete(@RequestBody NameRequest request) {
        var wasActive = request.name().equals(settingsService.activeProfile());
        settingsService.deleteProfile(request.name());
        if (wasActive) {
            colorService.restartIfRunning();
        }
        return list();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
