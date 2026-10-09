package ru.mkilord.colortomqttapp.application;

import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;

import java.util.List;
import java.util.Optional;

/**
 * Где лежат подключение и профили. Правила (последний профиль, занятое имя) проверяют сервисы.
 */
public interface SettingsStore {

    MqttConnection loadConnection();

    void saveConnection(MqttConnection connection);

    Optional<ProfileName> loadActiveProfile();

    void saveActiveProfile(ProfileName name);

    List<ProfileName> profileNames();

    Optional<ProfileSettings> loadProfile(ProfileName name);

    void saveProfile(ProfileName name, ProfileSettings settings);

    void renameProfile(ProfileName from, ProfileName to);

    void deleteProfile(ProfileName name);
}
