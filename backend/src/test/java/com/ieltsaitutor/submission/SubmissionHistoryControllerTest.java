package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ieltsaitutor.auth.AuthInterceptor;
import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;

class SubmissionHistoryControllerTest {
    @Test
    void learnerHistoryIsBoundedAndMappedWithoutAdminInternals() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        PracticeSubmission submission = submission(owner, id, "READING", SubmissionStatus.SUBMITTED);
        PracticeSubmissionRepository repository = mock(PracticeSubmissionRepository.class);
        when(repository.findHistory(owner, "reading", SubmissionStatus.SUBMITTED, 1, 20))
                .thenReturn(new SubmissionHistoryPage(List.of(submission), 1, 20, 21));

        SubmissionHistoryService service = new SubmissionHistoryService(repository);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new SubmissionHistoryController(service)).build();

        mvc.perform(get("/api/me/submissions?page=1&size=20&skill=reading&status=SUBMITTED")
                        .requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                                new AuthPrincipal(owner, "learner@example.com", "Learner", UserRole.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].skill").value("READING"))
                .andExpect(jsonPath("$.items[0].practiceVersionId").value("version-1"))
                .andExpect(jsonPath("$.items[0].scoreAvailable").value(false))
                .andExpect(jsonPath("$.total").value(21));
        verify(repository).findHistory(owner, "reading", SubmissionStatus.SUBMITTED, 1, 20);
    }

    @Test
    void rejectsInvalidBoundsAndDoesNotAllowUnboundedQueries() {
        SubmissionHistoryService service = new SubmissionHistoryService(mock(PracticeSubmissionRepository.class));
        UUID owner = UUID.randomUUID();

        assertThrows(SubmissionConflictException.class, () -> service.list(owner, null, null, -1, 20));
        assertThrows(SubmissionConflictException.class, () -> service.list(owner, null, null, 0, 51));
        assertThrows(SubmissionConflictException.class, () -> service.list(null, null, null, 0, 20));
    }

    private static PracticeSubmission submission(UUID owner, UUID id, String skill, SubmissionStatus status) {
        Instant now = Instant.now();
        return new PracticeSubmission(id, owner, skill, "set", "version-1", "published", 1, status,
                now, now, now, null, 0, "start", "submit", "hash", false, now, now);
    }
}
