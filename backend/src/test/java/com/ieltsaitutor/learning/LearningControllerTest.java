package com.ieltsaitutor.learning;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class LearningControllerTest {
    private final LearningProgressService service = mock(LearningProgressService.class);
    private MockMvc mvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mvc = MockMvcBuilders.standaloneSetup(new LearningController(service)).build();
    }

    @Test
    void progressIsScopedToAuthenticatedPrincipal() throws Exception {
        when(service.progress(userId)).thenReturn(new MemberProgress(userId,
                List.of(new SkillProgress("Reading", 6.5, 1), new SkillProgress("Listening", null, 0),
                        new SkillProgress("Writing", null, 0), new SkillProgress("Speaking", null, 0))));

        mvc.perform(get("/api/me/progress").requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(userId, "student@example.com", "Mai", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0].skill").value("Reading"))
                .andExpect(jsonPath("$.skills[0].band").value(6.5));
    }

    @Test
    void missingPrincipalIsRejected() throws Exception {
        mvc.perform(get("/api/me/progress")).andExpect(status().isUnauthorized());
    }
}
