package ru.mkilord.colortomqttapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.mkilord.colortomqttapp.TestProperties;
import ru.mkilord.colortomqttapp.controller.form.SettingsForm;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.util.LinkedHashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
@Import(SettingsControllerTest.Config.class)
class SettingsControllerTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        Properties properties() {
            return TestProperties.defaults();
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Properties properties;
    @MockitoBean
    private SettingsService settingsService;
    @MockitoBean
    private ColorService colorService;

    @BeforeEach
    void resetProperties() {
        properties.clear();
        properties.putAll(TestProperties.defaults());
    }

    @Test
    void showsCurrentSettings() throws Exception {
        mvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tcp://localhost:1883")))
                .andExpect(content().string(containsString("По допускам H, S, B")));
    }

    @Test
    void validSettingsAreSavedAndApplied() throws Exception {
        mvc.perform(formPost("topic", "home/ambilight"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));

        verify(settingsService).save(properties);
        verify(colorService).restartIfRunning();
        assertThat(properties.getProperty("topic")).isEqualTo("home/ambilight");
    }

    @Test
    void invalidSettingsAreRejectedWithMessage() throws Exception {
        mvc.perform(formPost("minHue", "300", "maxHue", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Минимум тона больше максимума")));

        verify(settingsService, never()).save(any());
        verify(colorService, never()).restartIfRunning();
    }

    @Test
    void resetRestoresDefaults() throws Exception {
        var defaults = TestProperties.defaults();
        when(settingsService.resetToDefaults()).thenReturn(defaults);

        mvc.perform(post("/settings/reset")).andExpect(redirectedUrl("/settings"));

        verify(colorService).restartIfRunning();
    }

    @Test
    void savedPasswordIsNotRenderedOnPage() throws Exception {
        properties.setProperty("username", "lamp");
        properties.setProperty("password", "very-secret-value");

        mvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lamp")))
                .andExpect(content().string(not(containsString("very-secret-value"))));
    }

    @Test
    void emptyPasswordFieldKeepsSavedPassword() throws Exception {
        properties.setProperty("username", "lamp");
        properties.setProperty("password", "secret");

        mvc.perform(formPost("username", "lamp", "password", ""))
                .andExpect(redirectedUrl("/settings"));

        assertThat(properties.getProperty("username")).isEqualTo("lamp");
        assertThat(properties.getProperty("password")).isEqualTo("secret");
    }

    @Test
    void credentialsFromFormAreSaved() throws Exception {
        mvc.perform(formPost("username", "lamp", "password", "pa55"))
                .andExpect(redirectedUrl("/settings"));

        assertThat(properties.getProperty("username")).isEqualTo("lamp");
        assertThat(properties.getProperty("password")).isEqualTo("pa55");
    }

    /**
     * POST со всеми полями формы из настроек по умолчанию; пары overrides заменяют отдельные поля.
     */
    private static MockHttpServletRequestBuilder formPost(String... overrides) {
        var form = SettingsForm.from(TestProperties.defaults());
        var params = new LinkedHashMap<String, String>();
        params.put("broker", form.getBroker());
        params.put("topic", form.getTopic());
        params.put("updatePeriod", String.valueOf(form.getUpdatePeriod()));
        params.put("holdTime", String.valueOf(form.getHoldTime()));
        params.put("screenWidth", String.valueOf(form.getScreenWidth()));
        params.put("screenHeight", String.valueOf(form.getScreenHeight()));
        params.put("cellSize", String.valueOf(form.getCellSize()));
        params.put("stateTracker", form.getStateTracker());
        params.put("detector", form.getDetector());
        params.put("processor", form.getProcessor());
        params.put("dominantMinShare", String.valueOf(form.getDominantMinShare()));
        params.put("sensitivity", String.valueOf(form.getSensitivity()));
        params.put("hueTolerance", String.valueOf(form.getHueTolerance()));
        params.put("saturationTolerance", String.valueOf(form.getSaturationTolerance()));
        params.put("brightnessTolerance", String.valueOf(form.getBrightnessTolerance()));
        params.put("modifyHue", String.valueOf(form.getModifyHue()));
        params.put("modifySaturation", String.valueOf(form.getModifySaturation()));
        params.put("modifyBrightness", String.valueOf(form.getModifyBrightness()));
        params.put("saturationBoost", String.valueOf(form.getSaturationBoost()));
        params.put("blackThreshold", String.valueOf(form.getBlackThreshold()));
        params.put("grayThreshold", String.valueOf(form.getGrayThreshold()));
        params.put("minHue", String.valueOf(form.getMinHue()));
        params.put("maxHue", String.valueOf(form.getMaxHue()));
        params.put("minSaturation", String.valueOf(form.getMinSaturation()));
        params.put("maxSaturation", String.valueOf(form.getMaxSaturation()));
        params.put("minBrightness", String.valueOf(form.getMinBrightness()));
        params.put("maxBrightness", String.valueOf(form.getMaxBrightness()));
        for (int i = 0; i + 1 < overrides.length; i += 2) {
            params.put(overrides[i], overrides[i + 1]);
        }
        var request = post("/settings");
        params.forEach(request::param);
        return request;
    }
}
