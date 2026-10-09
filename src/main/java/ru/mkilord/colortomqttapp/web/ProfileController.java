package ru.mkilord.colortomqttapp.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mkilord.colortomqttapp.application.ProfileService;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    public record Profiles(String active, List<String> profiles) {
    }

    public record NameRequest(String name) {
    }

    /** @param copyFrom профиль-образец или null для настроек по умолчанию */
    public record CreateRequest(String name, String copyFrom) {
    }

    public record RenameRequest(String from, String to) {
    }

    @GetMapping
    public Profiles list() {
        return new Profiles(profiles.active().value(), profiles.names().stream().map(ProfileName::value).toList());
    }

    @PostMapping("/activate")
    public Profiles activate(@RequestBody NameRequest request) {
        profiles.activate(ProfileName.of(request.name()));
        return list();
    }

    @PostMapping
    public Profiles create(@RequestBody CreateRequest request) {
        profiles.create(ProfileName.of(request.name()),
                request.copyFrom() == null ? null : ProfileName.of(request.copyFrom()));
        return list();
    }

    @PostMapping("/rename")
    public Profiles rename(@RequestBody RenameRequest request) {
        profiles.rename(ProfileName.of(request.from()), ProfileName.of(request.to()));
        return list();
    }

    @PostMapping("/delete")
    public Profiles delete(@RequestBody NameRequest request) {
        profiles.delete(ProfileName.of(request.name()));
        return list();
    }
}
