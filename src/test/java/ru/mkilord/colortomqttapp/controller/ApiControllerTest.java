package ru.mkilord.colortomqttapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.ColorStatus;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ColorService colorService;

    @Test
    void statusReturnsCurrentState() throws Exception {
        when(colorService.getStatus()).thenReturn(new ColorStatus(true, "#2f6fd0", "tcp://localhost:1883",
                "colorToMQTT", true, "{\"hue\":217,\"sat\":70,\"brightness\":84}",
                Instant.parse("2026-10-07T11:06:48Z"), null, null));

        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.color").value("#2f6fd0"))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.lastPayload").value("{\"hue\":217,\"sat\":70,\"brightness\":84}"))
                .andExpect(jsonPath("$.lastSentAt").value("2026-10-07T11:06:48Z"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void statusShowsErrorWhenStopped() throws Exception {
        when(colorService.getStatus()).thenReturn(new ColorStatus(false, null, "tcp://localhost:1883",
                "colorToMQTT", null, null, null, "Не удалось подключиться", null));

        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.error").value("Не удалось подключиться"));
    }
}
