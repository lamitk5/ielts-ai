package com.ieltsaitutor.learning.intelligence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class LearningIntelligenceControllerTest {
    private final LearningIntelligenceService service = mock(LearningIntelligenceService.class);
    private MockMvc mvc;
    private UUID userId;

    @BeforeEach
    void setUp() { userId = UUID.randomUUID(); mvc = MockMvcBuilders.standaloneSetup(new LearningIntelligenceController(service)).build(); }

    @Test
    void profileIsScopedToPrincipalAndExposesTruthfulEmptyState() throws Exception {
        when(service.profile(userId)).thenReturn(StudentLearningProfile.empty(userId, Instant.now()));

        mvc.perform(get("/api/learning/profile").requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(userId, "student@example.com", "Mai", UserRole.CUSTOMER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.evidenceState").value("INSUFFICIENT_DATA"));
    }

    @Test
    void missingPrincipalIsRejected() throws Exception {
        mvc.perform(get("/api/learning/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void roadmapCompletionUsesAuthenticatedOwner() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(service.completeRoadmapItem(userId, itemId)).thenReturn(true);
        mvc.perform(post("/api/learning/roadmap/items/{itemId}/complete", itemId)
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                                new AuthPrincipal(userId, "student@example.com", "Mai", UserRole.CUSTOMER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
    }
}
