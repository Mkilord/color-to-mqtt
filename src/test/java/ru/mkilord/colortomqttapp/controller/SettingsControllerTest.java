package ru.mkilord.colortomqttapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.mkilord.colortomqttapp.TestProperties;
import ru.mkilord.colortomqttapp.controller.form.SettingsForm;
import ru.mkilord.colortomqttapp.service.ColorService;
import ru.mkilord.colortomqttapp.service.SettingsService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
class SettingsControllerTest {

    private static final String MAIN = "Основной";
    private static final String GAMES = "Игры";
    private static final String MAIN_URL = SettingsController.pageUrl(MAIN);

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private SettingsService settingsService;
    @MockitoBean
    private ColorService colorService;

    /** Сохраненные настройки профилей, как их держал бы сервис. */
    private final java.util.Map<String, Properties> stored = new java.util.HashMap<>();

    @BeforeEach
    void setUp() {
        stored.clear();
        stored.put(MAIN, TestProperties.defaults());
        stored.put(GAMES, TestProperties.defaults());
        when(settingsService.activeProfile()).thenReturn(MAIN);
        when(settingsService.profiles()).thenReturn(List.of(GAMES, MAIN));
        when(settingsService.loadDefault()).thenAnswer(invocation -> TestProperties.defaults());
        when(settingsService.loadProfile(anyString())).thenAnswer(invocation -> copy(stored.get(invocation.<String>getArgument(0))));
        doAnswer(invocation -> {
            stored.put(invocation.getArgument(0), copy(invocation.getArgument(1)));
            return null;
        }).when(settingsService).saveProfile(anyString(), any());
    }

    private static Properties copy(Properties source) {
        var result = new Properties();
        result.putAll(source);
        return result;
    }

    @Test
    void showsActiveProfileWithProfileList() throws Exception {
        mvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("tcp://localhost:1883")))
                .andExpect(content().string(containsString("По допускам H, S, B")))
                .andExpect(content().string(containsString(GAMES)))
                .andExpect(content().string(containsString("settings-defaults")))
                .andExpect(content().string(containsString("mkilord")));
    }

    @Test
    void savesEditedProfileAndAppliesIt() throws Exception {
        mvc.perform(formPost(GAMES, "maxBrightness", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.pageUrl(GAMES)));

        verify(settingsService).saveProfile(eq(GAMES), any());
        verify(colorService).restartIfRunning();
        assertThat(stored.get(GAMES).getProperty("maxBrightness")).isEqualTo("30.0");
        assertThat(stored.get(MAIN).getProperty("maxBrightness")).isEqualTo("100");
    }

    @Test
    void unknownProfileFallsBackToActive() throws Exception {
        mvc.perform(formPost("Нет такого", "topic", "home/ambilight"))
                .andExpect(redirectedUrl(MAIN_URL));

        assertThat(stored.get(MAIN).getProperty("topic")).isEqualTo("home/ambilight");
    }

    @Test
    void invalidSettingsAreRejectedWithMessage() throws Exception {
        mvc.perform(formPost(MAIN, "minHue", "300", "maxHue", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Минимум тона больше максимума")));

        verify(settingsService, never()).saveProfile(anyString(), any());
        verify(colorService, never()).restartIfRunning();
    }

    @Test
    void fetchSaveAnswersJsonWithoutRedirect() throws Exception {
        mvc.perform(formPost(MAIN, "maxBrightness", "40").header("X-Requested-With", "fetch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Профиль «Основной» сохранен"))
                .andExpect(jsonPath("$.passwordSet").value(false));

        assertThat(stored.get(MAIN).getProperty("maxBrightness")).isEqualTo("40.0");
    }

    @Test
    void fetchSaveReportsErrorsByField() throws Exception {
        mvc.perform(formPost(MAIN, "minBrightness", "80", "maxBrightness", "20", "hueTolerance", "5")
                        .header("X-Requested-With", "fetch"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.brightnessRangeValid").value("Минимум яркости больше максимума"))
                .andExpect(jsonPath("$.errors.hueTolerance").value("До 1"));

        verify(settingsService, never()).saveProfile(anyString(), any());
    }

    @Test
    void resetRestoresProfileDefaults() throws Exception {
        when(settingsService.resetProfile(GAMES)).thenReturn(TestProperties.defaults());

        mvc.perform(post("/settings/reset").param("profile", GAMES))
                .andExpect(redirectedUrl(SettingsController.pageUrl(GAMES)));

        verify(settingsService).resetProfile(GAMES);
        verify(colorService).restartIfRunning();
    }

    @Test
    void savedPasswordIsNotRenderedOnPage() throws Exception {
        stored.get(MAIN).setProperty("username", "lamp");
        stored.get(MAIN).setProperty("password", "very-secret-value");

        mvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("lamp")))
                .andExpect(content().string(not(containsString("very-secret-value"))));
    }

    @Test
    void emptyPasswordFieldKeepsSavedPassword() throws Exception {
        stored.get(MAIN).setProperty("username", "lamp");
        stored.get(MAIN).setProperty("password", "secret");

        mvc.perform(formPost(MAIN, "username", "lamp", "password", ""))
                .andExpect(redirectedUrl(MAIN_URL));

        assertThat(stored.get(MAIN).getProperty("password")).isEqualTo("secret");
    }

    @Test
    void credentialsFromFormAreSaved() throws Exception {
        mvc.perform(formPost(MAIN, "username", "lamp", "password", "pa55"))
                .andExpect(redirectedUrl(MAIN_URL));

        assertThat(stored.get(MAIN).getProperty("username")).isEqualTo("lamp");
        assertThat(stored.get(MAIN).getProperty("password")).isEqualTo("pa55");
    }

    @Test
    void defaultsJsonHasNoCredentials() throws Exception {
        mvc.perform(get("/settings"))
                .andExpect(content().string(containsString("\"saturationBoost\":")))
                .andExpect(content().string(not(containsString("\"passwordSet\":"))))
                .andExpect(content().string(not(containsString("\"username\":"))));
    }

    /**
     * POST со всеми полями формы из настроек по умолчанию; пары overrides заменяют отдельные поля.
     */
    private static MockHttpServletRequestBuilder formPost(String profile, String... overrides) {
        var form = SettingsForm.from(TestProperties.defaults());
        var params = new LinkedHashMap<String, String>();
        params.put("broker", form.getBroker());
        params.put("topic", form.getTopic());
        params.put("updatePeriod", String.valueOf(form.getUpdatePeriod()));
        params.put("holdTime", String.valueOf(form.getHoldTime()));
        params.put("idlePeriod", String.valueOf(form.getIdlePeriod()));
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
        params.put("hueShiftRed", String.valueOf(form.getHueShiftRed()));
        params.put("hueShiftYellow", String.valueOf(form.getHueShiftYellow()));
        params.put("hueShiftGreen", String.valueOf(form.getHueShiftGreen()));
        params.put("hueShiftCyan", String.valueOf(form.getHueShiftCyan()));
        params.put("hueShiftBlue", String.valueOf(form.getHueShiftBlue()));
        params.put("hueShiftMagenta", String.valueOf(form.getHueShiftMagenta()));
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
        var request = post("/settings").param("profile", profile);
        params.forEach(request::param);
        return request;
    }
}
