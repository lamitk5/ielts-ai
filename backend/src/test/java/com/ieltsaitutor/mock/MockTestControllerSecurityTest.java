package com.ieltsaitutor.mock;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.CanonicalSubmissionService;
import com.ieltsaitutor.submission.SubmissionDraftSnapshot;

class MockTestControllerSecurityTest {

    private MockTestService mockTestService;
    private CanonicalSubmissionService submissionService;
    private MockMvc mvc;

    private final UUID ownerId = UUID.randomUUID();
    private final AuthPrincipal principal = new AuthPrincipal(ownerId, "learner@example.com", "Learner", UserRole.CUSTOMER);

    @BeforeEach
    void setUp() {
        mockTestService = mock(MockTestService.class);
        submissionService = mock(CanonicalSubmissionService.class);
        MockTestController controller = new MockTestController(mockTestService, submissionService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private MockTestSession sampleSession(UUID id, UUID userId, MockTestSessionStatus status) {
        Instant now = Instant.now();
        MockTestSection sec1 = new MockTestSection(
                UUID.randomUUID(), id, 0, "listening", "mock-listening-01", "v1.0",
                "listening-set-01", UUID.randomUUID(), 1800, MockTestSectionStatus.IN_PROGRESS, now, now
        );
        MockTestSection sec2 = new MockTestSection(
                UUID.randomUUID(), id, 1, "reading", "mock-reading-01", "v1.0",
                "reading-set-01", UUID.randomUUID(), 3600, MockTestSectionStatus.NOT_STARTED, now, now
        );
        return new MockTestSession(
                id, userId, "mock-full-academic-01", "v1.0", status,
                0, 10200, 120, now, now.plusSeconds(10200), null, now, now,
                List.of(sec1, sec2)
        );
    }

    @Test
    void startUsesAuthenticatedOwner() throws Exception {
        UUID sessionId = UUID.randomUUID();
        MockTestSession session = sampleSession(sessionId, ownerId, MockTestSessionStatus.IN_PROGRESS);

        when(mockTestService.startOrResume(eq(ownerId), eq("mock-full-academic-01"))).thenReturn(session);

        mvc.perform(post("/api/mock-tests/sessions/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"mockTestId": "mock-full-academic-01", "userId": "00000000-0000-0000-0000-000000000001"}
                        """)
                .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.userId").value(ownerId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.sections.length()").value(2));

        verify(mockTestService).startOrResume(ownerId, "mock-full-academic-01");
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(post("/api/mock-tests/sessions/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"mockTestId": "mock-full-academic-01"}
                        """))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/mock-tests/sessions/active"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/mock-tests/sessions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getActiveSessionReturnsActiveSession() throws Exception {
        UUID sessionId = UUID.randomUUID();
        MockTestSession session = sampleSession(sessionId, ownerId, MockTestSessionStatus.IN_PROGRESS);

        when(mockTestService.listUserSessions(ownerId)).thenReturn(List.of(session));

        mvc.perform(get("/api/mock-tests/sessions/active")
                .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void getSessionRequiresOwner() throws Exception {
        UUID sessionId = UUID.randomUUID();
        MockTestSession session = sampleSession(sessionId, ownerId, MockTestSessionStatus.IN_PROGRESS);

        when(mockTestService.getSession(ownerId, sessionId)).thenReturn(session);

        mvc.perform(get("/api/mock-tests/sessions/" + sessionId)
                .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()));

        verify(mockTestService).getSession(ownerId, sessionId);
    }

    @Test
    void executeCommandTransitionsSession() throws Exception {
        UUID sessionId = UUID.randomUUID();
        MockTestSession session = sampleSession(sessionId, ownerId, MockTestSessionStatus.PAUSED);

        when(mockTestService.executeCommand(ownerId, sessionId, MockTestCommand.PAUSE)).thenReturn(session);

        mvc.perform(post("/api/mock-tests/sessions/" + sessionId + "/command")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"command": "PAUSE"}
                        """)
                .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        verify(mockTestService).executeCommand(ownerId, sessionId, MockTestCommand.PAUSE);
    }

    @Test
    void executeCommandReturnsConflictWhenInvalidTransition() throws Exception {
        UUID sessionId = UUID.randomUUID();

        when(mockTestService.executeCommand(ownerId, sessionId, MockTestCommand.RESUME))
                .thenThrow(new MockConflictException("Cannot resume"));

        mvc.perform(post("/api/mock-tests/sessions/" + sessionId + "/command")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"command": "RESUME"}
                        """)
                .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isConflict());
    }

    @Test
    void autosaveSectionDraftDelegatesToCanonicalSubmissionService() throws Exception {
        UUID sessionId = UUID.randomUUID();
        MockTestSession session = sampleSession(sessionId, ownerId, MockTestSessionStatus.IN_PROGRESS);
        UUID subId = session.sections().get(0).submissionId();

        when(mockTestService.getSession(ownerId, sessionId)).thenReturn(session);
        when(submissionService.autosave(eq(ownerId), eq(subId), any(), eq(1L), any()))
                .thenReturn(new SubmissionDraftSnapshot(subId, ownerId, Map.of("q1", "A"), 2L, "draft-key-1", Instant.now()));

        mvc.perform(post("/api/mock-tests/sessions/" + sessionId + "/sections/0/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"answers": {"q1": "A"}, "expectedRevision": 1, "idempotencyKey": "draft-key-1"}
                        """)
                .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE, principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value(2));

        verify(submissionService).autosave(eq(ownerId), eq(subId), eq(Map.of("q1", "A")), eq(1L), eq("draft-key-1"));
    }
}
