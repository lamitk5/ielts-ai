package com.ieltsaitutor.submission;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class SubmissionControllerSecurityTest {
    @Test
    void startUsesAuthenticatedOwnerAndDoesNotTrustClientIdentityOrScore() throws Exception {
        UUID owner = UUID.randomUUID();
        PracticeSubmission submission = submission(owner);
        CanonicalSubmissionService service = mock(CanonicalSubmissionService.class);
        when(service.start(eq(owner), any(SubmissionStartCommand.class))).thenReturn(submission);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SubmissionController(service)).build();

        mvc.perform(post("/api/submissions").contentType(MediaType.APPLICATION_JSON).content("""
                {"publishedSetId":"published-reading-1","skill":"reading","idempotencyKey":"start-1",
                 "userId":"00000000-0000-0000-0000-000000000001","score":99,"answerKey":"A"}
                """).requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(owner, "owner@example.com", "Owner", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(submission.id().toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(service).start(eq(owner), eq(new SubmissionStartCommand("published-reading-1", "reading", "start-1")));
    }

    @Test
    void objectReadRequiresTheAuthenticatedPrincipal() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        CanonicalSubmissionService service = mock(CanonicalSubmissionService.class);
        when(service.get(owner, id)).thenReturn(submission(owner, id));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SubmissionController(service)).build();

        mvc.perform(get("/api/submissions/" + id).requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(owner, "owner@example.com", "Owner", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(get("/api/submissions/" + id)).andExpect(status().isUnauthorized());
        verify(service).get(owner, id);
    }

    private PracticeSubmission submission(UUID owner) {
        return submission(owner, UUID.randomUUID());
    }

    private PracticeSubmission submission(UUID owner, UUID id) {
        Instant now = Instant.now();
        return new PracticeSubmission(id, owner, "READING", "published-reading-1", "version-1",
                "published-reading-1", 1, SubmissionStatus.IN_PROGRESS, now, now, null, null, 0,
                "start-1", null, null, false, now, now);
    }
}
