package com.ieltsaitutor.practice.attempt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class AttemptControllerTest {
    private final AttemptService service = mock(AttemptService.class);
    private MockMvc mvc;
    private UUID userId;

    @BeforeEach
    void setUp() { mvc = MockMvcBuilders.standaloneSetup(new AttemptController(service)).build(); userId = UUID.randomUUID(); }

    @Test
    void attemptDetailIsScopedToAuthenticatedPrincipal() throws Exception {
        UUID attemptId = UUID.randomUUID();
        PracticeAttempt attempt = new PracticeAttempt(attemptId, userId, "reading-foundation-01", "v1", "reading",
                AttemptStatus.IN_PROGRESS, Map.of("q1", "B"), null, null, java.time.Instant.now(), null, "{}", "key");
        when(service.get(userId, attemptId)).thenReturn(attempt);

        mvc.perform(get("/api/attempts/" + attemptId)
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                                new AuthPrincipal(userId, "student@example.com", "Student", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(attemptId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
