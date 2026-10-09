package ru.mkilord.colortomqttapp.application;

import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class InMemorySettingsStore implements SettingsStore {

    MqttConnection connection = new MqttConnection("tcp://localhost:1883", "screen/color", "", "");
    ProfileName active;
    final Map<ProfileName, ProfileSettings> profiles = new LinkedHashMap<>();

    @Override
    public MqttConnection loadConnection() {
        return connection;
    }

    @Override
    public void saveConnection(MqttConnection value) {
        connection = value;
    }

    @Override
    public Optional<ProfileName> loadActiveProfile() {
        return Optional.ofNullable(active);
    }

    @Override
    public void saveActiveProfile(ProfileName name) {
        active = name;
    }

    @Override
    public List<ProfileName> profileNames() {
        var names = new ArrayList<>(profiles.keySet());
        names.sort(Comparator.comparing(ProfileName::value));
        return names;
    }

    @Override
    public Optional<ProfileSettings> loadProfile(ProfileName name) {
        return Optional.ofNullable(profiles.get(name));
    }

    @Override
    public void saveProfile(ProfileName name, ProfileSettings settings) {
        profiles.put(name, settings);
    }

    @Override
    public void renameProfile(ProfileName from, ProfileName to) {
        profiles.put(to, profiles.remove(from));
    }

    @Override
    public void deleteProfile(ProfileName name) {
        profiles.remove(name);
    }
}
