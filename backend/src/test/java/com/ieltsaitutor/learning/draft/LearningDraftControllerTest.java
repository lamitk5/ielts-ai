package com.ieltsaitutor.learning.draft;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthExceptionHandler;
import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthService;
import com.ieltsaitutor.auth.AuthUser;
import com.ieltsaitutor.auth.UserRole;

class LearningDraftControllerTest {
    private static final String BASE_PATH = "/api/learning/drafts";
    private final AuthService auth = mock(AuthService.class);
    private final LearningDraftService service = mock(LearningDraftService.class);
    private MockMvc mvc;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LearningDraftController(service))
                .addInterceptors(new AuthInterceptor(auth))
                .setControllerAdvice(new AuthExceptionHandler())
                .build();
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mvc.perform(get(BASE_PATH + "/current?skill=WRITING&referenceId=task-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));

        mvc.perform(put(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skill\":\"WRITING\",\"referenceId\":\"task-1\",\"contentSnapshot\":\"draft\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void authenticatedUserCanFetchAndSaveDraft() throws Exception {
        String token = "Bearer test-token";
        when(auth.authenticate(token)).thenReturn(new AuthUser(userId, "test@example.com", "Test", "hash", UserRole.CUSTOMER, Instant.now()));

        LearningDraft draft = new LearningDraft(
                UUID.randomUUID(), userId, "WRITING", "task-1",
                "My current drafted paragraph", 1L, LearningDraft.DraftStatus.ACTIVE,
                Instant.now(), Instant.now(), null
        );

        when(service.getCurrentDraft(userId, "WRITING", "task-1")).thenReturn(Optional.of(draft));
        when(service.saveDraft(eq(userId), eq("WRITING"), eq("task-1"), eq("Updated text"), eq(1L)))
                .thenReturn(new LearningDraft(
                        draft.id(), userId, "WRITING", "task-1",
                        "Updated text", 2L, LearningDraft.DraftStatus.ACTIVE,
                        Instant.now(), Instant.now(), null
                ));

        mvc.perform(get(BASE_PATH + "/current?skill=WRITING&referenceId=task-1")
                .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skill").value("WRITING"))
                .andExpect(jsonPath("$.referenceId").value("task-1"))
                .andExpect(jsonPath("$.contentSnapshot").value("My current drafted paragraph"))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(put(BASE_PATH)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skill\":\"WRITING\",\"referenceId\":\"task-1\",\"contentSnapshot\":\"Updated text\",\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.contentSnapshot").value("Updated text"));
    }

    @Test
    void versionConflictReturns409() throws Exception {
        String token = "Bearer test-token";
        when(auth.authenticate(token)).thenReturn(new AuthUser(userId, "test@example.com", "Test", "hash", UserRole.CUSTOMER, Instant.now()));

        when(service.saveDraft(any(), any(), any(), any(), any()))
                .thenThrow(new DraftConflictException("VERSION_CONFLICT", "Bản nháp đã được cập nhật từ thiết bị khác."));

        mvc.perform(put(BASE_PATH)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skill\":\"WRITING\",\"referenceId\":\"task-1\",\"contentSnapshot\":\"Conflicting text\",\"expectedVersion\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VERSION_CONFLICT"));
    }
}
