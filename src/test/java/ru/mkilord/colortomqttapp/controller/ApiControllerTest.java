package ru.mkilord.colortomqttapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import ru.mkilord.colortomqttapp.core.HSBColor;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.ColorStatus;

import java.time.Instant;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                Instant.parse("2026-10-07T11:06:48Z"), null, null,
                new ColorStatus.Performance(58.2, 9.4, 0.3, false)));

        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(true))
                .andExpect(jsonPath("$.color").value("#2f6fd0"))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.lastPayload").value("{\"hue\":217,\"sat\":70,\"brightness\":84}"))
                .andExpect(jsonPath("$.lastSentAt").value("2026-10-07T11:06:48Z"))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.performance.fps").value(58.2))
                .andExpect(jsonPath("$.performance.captureMs").value(9.4));
    }

    @Test
    void statusShowsErrorWhenStopped() throws Exception {
        when(colorService.getStatus()).thenReturn(new ColorStatus(false, null, "tcp://localhost:1883",
                "colorToMQTT", null, null, null, "Не удалось подключиться", null, null));

        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false))
                .andExpect(jsonPath("$.error").value("Не удалось подключиться"));
    }

    @Test
    void testColorIsSentAsIs() throws Exception {
        when(colorService.sendTestColor(new HSBColor(120, 100, 40))).thenReturn("{\"hue\":120,\"sat\":100,\"brightness\":40}");

        mvc.perform(post("/api/test-color").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hue\":120,\"sat\":100,\"brightness\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("{\"hue\":120,\"sat\":100,\"brightness\":40}"));
    }

    @Test
    void testColorOutOfRangeIsRejected() throws Exception {
        mvc.perform(post("/api/test-color").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hue\":400,\"sat\":100,\"brightness\":40}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testColorReportsBrokerError() throws Exception {
        when(colorService.sendTestColor(new HSBColor(0, 100, 50))).thenThrow(new IllegalStateException("Нет соединения с брокером"));

        mvc.perform(post("/api/test-color").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hue\":0,\"sat\":100,\"brightness\":50}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Не удалось отправить: Нет соединения с брокером"));
    }
}
