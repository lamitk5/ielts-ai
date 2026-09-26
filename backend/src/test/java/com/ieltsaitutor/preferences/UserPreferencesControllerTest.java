package com.ieltsaitutor.preferences;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthException;
import com.ieltsaitutor.auth.AuthExceptionHandler;
import com.ieltsaitutor.auth.AuthService;
import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.UserRole;

class UserPreferencesControllerTest {
    private static final String PATH = "/api/user/preferences";
    private static final String VALID_BODY = """
            {"themeMode":"DARK","accentPreset":"SAPPHIRE","fontScale":"LARGE","density":"COMFORTABLE",
             "reduceMotion":"ALLOWED","proactiveAiEnabled":true,"crossHighlightEnabled":false,
             "timerDefaultEnabled":true,"readingSplitRatio":50,"writingSplitRatio":60,"version":0}
            """;
    private final AuthService auth = mock(AuthService.class);
    private final UserPreferencesService service = mock(UserPreferencesService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UserPreferencesController(service))
                .addInterceptors(new AuthInterceptor(auth))
                .setControllerAdvice(new UserPreferencesExceptionHandler(), new AuthExceptionHandler())
                .build();
    }

    @Test
    void unauthenticatedGetAndPutReturnNormalized401() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void authenticatedGetUsesPrincipalAndReturnsFullRecord() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(service.get(userId)).thenReturn(new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                false, true, false, 40, 40, 0L, Instant.parse("2026-01-01T00:00:00Z")));

        mvc.perform(get(PATH).header("Authorization", "Bearer valid").param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("SYSTEM"))
                .andExpect(jsonPath("$.accentPreset").value("GOLD"))
                .andExpect(jsonPath("$.fontScale").value("DEFAULT"))
                .andExpect(jsonPath("$.density").value("DEFAULT"))
                .andExpect(jsonPath("$.reduceMotion").value("SYSTEM"))
                .andExpect(jsonPath("$.proactiveAiEnabled").value(false))
                .andExpect(jsonPath("$.crossHighlightEnabled").value(true))
                .andExpect(jsonPath("$.timerDefaultEnabled").value(false))
                .andExpect(jsonPath("$.readingSplitRatio").value(40))
                .andExpect(jsonPath("$.writingSplitRatio").value(40))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void authenticatedPutUsesPrincipalAndReturnsServerRecord() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(service.update(org.mockito.ArgumentMatchers.eq(userId), any(UserPreferences.class), org.mockito.ArgumentMatchers.eq(0L)))
                .thenReturn(new UserPreferences("DARK", "SAPPHIRE", "LARGE", "COMFORTABLE", "ALLOWED",
                        true, false, true, 50, 60, 1L, Instant.parse("2026-01-02T00:00:00Z")));

        mvc.perform(put(PATH).header("Authorization", "Bearer valid").param("userId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.accentPreset").value("SAPPHIRE"))
                .andExpect(jsonPath("$.fontScale").value("LARGE"))
                .andExpect(jsonPath("$.density").value("COMFORTABLE"))
                .andExpect(jsonPath("$.reduceMotion").value("ALLOWED"))
                .andExpect(jsonPath("$.proactiveAiEnabled").value(true))
                .andExpect(jsonPath("$.crossHighlightEnabled").value(false))
                .andExpect(jsonPath("$.timerDefaultEnabled").value(true))
                .andExpect(jsonPath("$.readingSplitRatio").value(50))
                .andExpect(jsonPath("$.writingSplitRatio").value(60))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void malformedAndIncompletePutReturnNormalized400() throws Exception {
        authenticate(UUID.randomUUID());
        for (String body : new String[] { "{bad", "{}", VALID_BODY.replace("\"version\":0", "\"version\":null") }) {
            mvc.perform(put(PATH).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("PREFERENCES_INVALID_REQUEST"));
        }
    }

    @Test
    void rejectedPreferenceUpdateReturnsNormalizedValidationError() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(service.update(org.mockito.ArgumentMatchers.eq(userId), any(UserPreferences.class), org.mockito.ArgumentMatchers.eq(0L)))
                .thenThrow(new AuthException("PREFERENCES_INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Thiết lập chưa hợp lệ."));

        mvc.perform(put(PATH).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PREFERENCES_INVALID_REQUEST"));
    }

    @Test
    void stalePreferenceUpdateReturnsNormalizedConflict() throws Exception {
        UUID userId = UUID.randomUUID();
        authenticate(userId);
        when(service.update(org.mockito.ArgumentMatchers.eq(userId), any(UserPreferences.class), org.mockito.ArgumentMatchers.eq(0L)))
                .thenThrow(new AuthException("VERSION_CONFLICT", HttpStatus.CONFLICT, "Thiết lập đã được thay đổi."));

        mvc.perform(put(PATH).header("Authorization", "Bearer valid").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VERSION_CONFLICT"));
    }

    private void authenticate(UUID userId) {
        when(auth.authenticate("Bearer valid")).thenReturn(new AuthUser(userId, "student@example.com", "Mai", "hash",
                UserRole.CUSTOMER, Instant.parse("2026-01-01T00:00:00Z")));
    }
}
