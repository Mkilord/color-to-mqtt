package ru.mkilord.colortomqttapp.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.mkilord.colortomqttapp.application.CaptureService;
import ru.mkilord.colortomqttapp.application.ConnectionService;
import ru.mkilord.colortomqttapp.application.ProfileService;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.web.form.SettingsForm;

@Controller
public class PageController {

    private final CaptureService capture;
    private final ProfileService profiles;
    private final ConnectionService connections;
    private final SettingsPageModel settingsPage;

    public PageController(CaptureService capture, ProfileService profiles, ConnectionService connections,
                          SettingsPageModel settingsPage) {
        this.capture = capture;
        this.profiles = profiles;
        this.connections = connections;
        this.settingsPage = settingsPage;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("status", capture.status());
        model.addAttribute("profiles", profiles.names().stream().map(ProfileName::value).toList());
        model.addAttribute("activeProfile", profiles.active().value());
        return "index";
    }

    @GetMapping("/settings")
    public String settings(@RequestParam(required = false) String profile, Model model) {
        var name = settingsPage.resolve(profile);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", SettingsForm.of(profiles.settings(name), connections.current()));
        }
        settingsPage.fill(model, name);
        return "settings";
    }
}
