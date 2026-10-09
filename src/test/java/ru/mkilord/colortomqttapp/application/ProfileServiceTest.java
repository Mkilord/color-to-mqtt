package ru.mkilord.colortomqttapp.application;

import org.junit.jupiter.api.Test;
import ru.mkilord.colortomqttapp.domain.error.ProfileConflictException;
import ru.mkilord.colortomqttapp.domain.error.ProfileNotFoundException;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.CaptureSettings;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileServiceTest {

    private static final ProfileName GAMES = ProfileName.of("Игры");
    private static final ProfileSettings WIDE = ProfileSettings.DEFAULTS.toBuilder()
            .capture(CaptureSettings.builder().width(900).build())
            .build();

    private final InMemorySettingsStore store = new InMemorySettingsStore();
    private final List<Object> events = new ArrayList<>();
    private final ProfileService service = new ProfileService(store, events::add);

    @Test
    void defaultProfileIsCreatedOnFirstAccess() {
        assertThat(service.names()).containsExactly(ProfileName.DEFAULT);
        assertThat(service.active()).isEqualTo(ProfileName.DEFAULT);
        assertThat(service.activeSettings()).isEqualTo(ProfileSettings.DEFAULTS);
    }

    @Test
    void createCopiesSourceProfile() {
        service.save(ProfileName.DEFAULT, WIDE);
        events.clear();

        service.create(GAMES, ProfileName.DEFAULT);

        assertThat(service.settings(GAMES)).isEqualTo(WIDE);
        assertThat(events).isEmpty();
    }

    @Test
    void duplicateNameIgnoresCase() {
        service.create(GAMES, null);

        assertThatThrownBy(() -> service.create(ProfileName.of("игры"), null))
                .isInstanceOf(ProfileConflictException.class);
    }

    @Test
    void savingInactiveProfileDoesNotRestartCapture() {
        service.create(GAMES, null);

        service.save(GAMES, WIDE);
        assertThat(events).isEmpty();

        service.save(ProfileName.DEFAULT, WIDE);
        assertThat(events).hasSize(1);
    }

    @Test
    void activateAnnouncesChangeOnce() {
        service.create(GAMES, null);

        service.activate(GAMES);
        service.activate(GAMES);

        assertThat(service.active()).isEqualTo(GAMES);
        assertThat(events).hasSize(1);
    }

    @Test
    void renameKeepsActiveProfile() {
        service.create(GAMES, null);
        service.activate(GAMES);

        service.rename(GAMES, ProfileName.of("Кино"));

        assertThat(service.active()).isEqualTo(ProfileName.of("Кино"));
        assertThat(service.exists(GAMES)).isFalse();
    }

    @Test
    void renameToOwnNameWithOtherCaseIsAllowed() {
        service.create(GAMES, null);

        service.rename(GAMES, ProfileName.of("ИГРЫ"));

        assertThat(service.names()).contains(ProfileName.of("ИГРЫ"));
    }

    @Test
    void lastProfileCannotBeDeleted() {
        assertThatThrownBy(() -> service.delete(ProfileName.DEFAULT))
                .isInstanceOf(ProfileConflictException.class);
    }

    @Test
    void deletingActiveProfileSwitchesToAnother() {
        service.create(GAMES, null);
        service.activate(GAMES);
        events.clear();

        service.delete(GAMES);

        assertThat(service.active()).isEqualTo(ProfileName.DEFAULT);
        assertThat(events).hasSize(1);
    }

    @Test
    void unknownProfileIsReported() {
        assertThatThrownBy(() -> service.activate(GAMES)).isInstanceOf(ProfileNotFoundException.class);
        assertThatThrownBy(() -> service.save(GAMES, WIDE)).isInstanceOf(ProfileNotFoundException.class);
    }
}
