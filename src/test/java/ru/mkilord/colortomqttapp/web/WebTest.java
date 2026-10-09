package ru.mkilord.colortomqttapp.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.mkilord.colortomqttapp.application.ConnectionService;
import ru.mkilord.colortomqttapp.application.ProfileService;
import ru.mkilord.colortomqttapp.domain.connection.MqttConnection;
import ru.mkilord.colortomqttapp.domain.profile.ProfileName;
import ru.mkilord.colortomqttapp.domain.settings.ProfileSettings;
import ru.mkilord.colortomqttapp.web.form.SettingsForm;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebTest {

    private static final String SECRET = "very-secret-password";

    @TempDir
    static Path storage;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("app.storage-dir", storage::toString);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    ProfileService profiles;
    @Autowired
    ConnectionService connections;

    @BeforeEach
    void reset() {
        profiles.names().stream()
                .filter(name -> !name.equals(ProfileName.DEFAULT))
                .forEach(profiles::delete);
        profiles.reset(ProfileName.DEFAULT);
        profiles.activate(ProfileName.DEFAULT);
        connections.update(new MqttConnection("tcp://localhost:1883", "screen/color", "", ""));
    }

    @Test
    void pagesRenderWithoutPassword() throws Exception {
        connections.update(new MqttConnection("tcp://localhost:1883", "screen/color", "user", SECRET));

        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(SECRET))))
                .andExpect(content().string(not(containsString("\"passwordSet\":"))));
    }

    @Test
    void fetchSaveStoresProfile() throws Exception {
        mvc.perform(form(Map.of("screenWidth", "640")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("сохранен")))
                .andExpect(jsonPath("$.passwordSet").value(false));

        assertThat(profiles.activeSettings().capture().width()).isEqualTo(640);
    }

    @Test
    void fetchSaveReportsErrorsByFormField() throws Exception {
        mvc.perform(form(Map.of("screenWidth", "0", "minHue", "200", "maxHue", "100", "broker", "localhost")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.screenWidth").exists())
                .andExpect(jsonPath("$.errors.maxHue").exists())
                .andExpect(jsonPath("$.errors.broker").exists());

        assertThat(profiles.activeSettings()).isEqualTo(ProfileSettings.DEFAULTS);
    }

    @Test
    void fetchSaveReportsNotANumber() throws Exception {
        mvc.perform(form(Map.of("cellSize", "abc")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.cellSize").value("Введите число"));
    }

    @Test
    void emptyPasswordKeepsSavedOne() throws Exception {
        connections.update(new MqttConnection("tcp://localhost:1883", "screen/color", "user", SECRET));

        mvc.perform(form(Map.of("username", "user", "password", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordSet").value(true));

        assertThat(connections.current().password()).isEqualTo(SECRET);
    }

    @Test
    void htmlSaveRedirectsOrShowsErrors() throws Exception {
        mvc.perform(plainForm(Map.of("screenWidth", "800")))
                .andExpect(redirectedUrl(SettingsPageModel.url(ProfileName.DEFAULT.value())));
        assertThat(profiles.activeSettings().capture().width()).isEqualTo(800);

        mvc.perform(plainForm(Map.of("screenWidth", "0")))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "screenWidth"));
    }

    @Test
    void profileLifecycle() throws Exception {
        mvc.perform(json(post("/api/profiles"), "{\"name\":\"Игры\",\"copyFrom\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profiles.length()").value(2));
        mvc.perform(json(post("/api/profiles"), "{\"name\":\"игры\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(json(post("/api/profiles/activate"), "{\"name\":\"Игры\"}"))
                .andExpect(jsonPath("$.active").value("Игры"));
        mvc.perform(json(post("/api/profiles/rename"), "{\"from\":\"Игры\",\"to\":\"Кино\"}"))
                .andExpect(jsonPath("$.active").value("Кино"));
        mvc.perform(json(post("/api/profiles/delete"), "{\"name\":\"Кино\"}"))
                .andExpect(jsonPath("$.active").value(ProfileName.DEFAULT.value()));
        mvc.perform(json(post("/api/profiles/delete"), "{\"name\":\"Кино\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(json(post("/api/profiles"), "{\"name\":\"CON\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void statusAndInvalidSnapshot() throws Exception {
        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running").value(false));
        mvc.perform(get("/api/snapshot").param("width", "0").param("height", "10"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder form(Map<String, String> overrides) {
        return plainForm(overrides).header(SettingsController.FETCH_HEADER, "fetch");
    }

    private MockHttpServletRequestBuilder plainForm(Map<String, String> overrides) {
        Map<String, Object> values = json.convertValue(
                SettingsForm.of(profiles.activeSettings(), connections.current()), new TypeReference<>() {
                });
        var params = new LinkedHashMap<String, String>();
        values.forEach((key, value) -> params.put(key, value == null ? "" : value.toString()));
        params.putAll(overrides);
        var request = post("/settings").contentType(MediaType.APPLICATION_FORM_URLENCODED);
        params.forEach((key, value) -> request.param(key, value));
        return request;
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
