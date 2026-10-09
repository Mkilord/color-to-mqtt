package ru.mkilord.colortomqttapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private SettingsService settingsService;
    @MockitoBean
    private ColorService colorService;

    @Test
    void listsProfiles() throws Exception {
        when(settingsService.activeProfile()).thenReturn("Игры");
        when(settingsService.profiles()).thenReturn(List.of("Игры", "Основной"));

        mvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value("Игры"))
                .andExpect(jsonPath("$.profiles[1]").value("Основной"));
    }

    @Test
    void activatingAnotherProfileRestartsCapture() throws Exception {
        when(settingsService.activeProfile()).thenReturn("Основной");
        when(settingsService.profiles()).thenReturn(List.of("Игры", "Основной"));

        mvc.perform(post("/api/profiles/activate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Игры\"}"))
                .andExpect(status().isOk());

        verify(settingsService).activate("Игры");
        verify(colorService).restartIfRunning();
    }

    @Test
    void activatingActiveProfileDoesNothing() throws Exception {
        when(settingsService.activeProfile()).thenReturn("Игры");
        when(settingsService.profiles()).thenReturn(List.of("Игры"));

        mvc.perform(post("/api/profiles/activate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Игры\"}"))
                .andExpect(status().isOk());

        verify(settingsService, never()).activate("Игры");
        verify(colorService, never()).restartIfRunning();
    }

    @Test
    void creatingProfileWithBadNameReturnsMessage() throws Exception {
        doThrow(new IllegalArgumentException("Профиль «Игры» уже есть")).when(settingsService).createProfile("Игры", null);

        mvc.perform(post("/api/profiles").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Игры\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Профиль «Игры» уже есть"));
    }

    @Test
    void deletingActiveProfileRestartsCapture() throws Exception {
        when(settingsService.activeProfile()).thenReturn("Игры", "Основной");
        when(settingsService.profiles()).thenReturn(List.of("Основной"));

        mvc.perform(post("/api/profiles/delete").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Игры\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value("Основной"));

        verify(settingsService).deleteProfile("Игры");
        verify(colorService).restartIfRunning();
    }
}
