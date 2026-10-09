package ru.mkilord.colortomqttapp.application;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.mkilord.colortomqttapp.domain.error.ProfileConflictException;
import ru.mkilord.colortomqttapp.domain.error.ProfileNotFoundException;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;

import java.util.List;

/**
 * Профили настроек: список, активный профиль, создание, переименование, удаление, сохранение.
 * Изменение активного профиля публикует {@link SettingsChangedEvent}, захват перезапускается.
 * Событие публикуется после выхода из блокировки: слушатель сам обращается к этому сервису.
 */
@Service
public class ProfileService {

    private final SettingsStore store;
    private final ApplicationEventPublisher events;

    public ProfileService(SettingsStore store, ApplicationEventPublisher events) {
        this.store = store;
        this.events = events;
    }

    public synchronized List<ProfileName> names() {
        ensureAny();
        return store.profileNames();
    }

    public synchronized ProfileName active() {
        var names = names();
        return store.loadActiveProfile().filter(names::contains).orElse(names.get(0));
    }

    public synchronized ProfileSettings activeSettings() {
        return settings(active());
    }

    public synchronized ProfileSettings settings(ProfileName name) {
        return store.loadProfile(name).orElseThrow(() -> new ProfileNotFoundException(name.value()));
    }

    public synchronized boolean exists(ProfileName name) {
        return names().contains(name);
    }

    public void save(ProfileName name, ProfileSettings settings) {
        boolean active;
        synchronized (this) {
            requireExisting(name);
            store.saveProfile(name, settings);
            active = name.equals(active());
        }
        if (active) {
            events.publishEvent(new SettingsChangedEvent("профиль «" + name + "» сохранен"));
        }
    }

    public void reset(ProfileName name) {
        save(name, ProfileSettings.DEFAULTS);
    }

    public void activate(ProfileName name) {
        synchronized (this) {
            requireExisting(name);
            if (name.equals(active())) {
                return;
            }
            store.saveActiveProfile(name);
        }
        events.publishEvent(new SettingsChangedEvent("выбран профиль «" + name + "»"));
    }

    /**
     * @param copyFrom профиль-образец или null для настроек по умолчанию
     */
    public synchronized void create(ProfileName name, ProfileName copyFrom) {
        requireFree(name, null);
        var settings = copyFrom == null ? ProfileSettings.DEFAULTS : settings(copyFrom);
        store.saveProfile(name, settings);
    }

    public synchronized void rename(ProfileName from, ProfileName to) {
        requireExisting(from);
        if (from.equals(to)) {
            return;
        }
        requireFree(to, from);
        var wasActive = from.equals(active());
        store.renameProfile(from, to);
        if (wasActive) {
            store.saveActiveProfile(to);
        }
    }

    public void delete(ProfileName name) {
        boolean wasActive;
        synchronized (this) {
            requireExisting(name);
            if (names().size() == 1) {
                throw new ProfileConflictException("Это последний профиль, его нельзя удалить");
            }
            wasActive = name.equals(active());
            store.deleteProfile(name);
            if (wasActive) {
                store.saveActiveProfile(store.profileNames().get(0));
            }
        }
        if (wasActive) {
            events.publishEvent(new SettingsChangedEvent("удален активный профиль «" + name + "»"));
        }
    }

    private void ensureAny() {
        if (store.profileNames().isEmpty()) {
            store.saveProfile(ProfileName.DEFAULT, ProfileSettings.DEFAULTS);
            store.saveActiveProfile(ProfileName.DEFAULT);
        }
    }

    private void requireExisting(ProfileName name) {
        if (!names().contains(name)) {
            throw new ProfileNotFoundException(name.value());
        }
    }

    /** Имена сравниваются без учета регистра: в Windows это один файл. */
    private void requireFree(ProfileName name, ProfileName except) {
        for (var existing : names()) {
            if (!existing.equals(except) && existing.sameFileAs(name)) {
                throw new ProfileConflictException("Профиль «" + existing + "» уже есть");
            }
        }
    }
}
