package com.ieltsaitutor.assessment.objective;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.ieltsaitutor.submission.CanonicalSubmissionService;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.SubmissionConflictException;
import com.ieltsaitutor.submission.SubmissionStatus;

class ObjectiveResultControllerTest {
    @Test
    void exposesQuestionReviewOnlyAfterTrustedGrade() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        CanonicalSubmissionService service = mock(CanonicalSubmissionService.class);
        QuestionResultRepository results = mock(QuestionResultRepository.class);
        when(service.get(owner, id)).thenReturn(submission(owner, id, SubmissionStatus.GRADED));
        when(results.findByOwnedSubmission(owner, id)).thenReturn(List.of(new QuestionResult(UUID.randomUUID(), id, owner, "q1",
                "DETAIL", "B", "b", "A", false, "passage:p1", "Because", "objective-v1", now)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ObjectiveResultController(service, results)).build();

        mvc.perform(get("/api/submissions/" + id + "/result").requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(owner, "learner@example.com", "Learner", UserRole.CUSTOMER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.questionResults[0].correctAnswer").value("A"));
    }

    @Test
    void doesNotExposeAnswerKeyBeforeGrading() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        CanonicalSubmissionService service = mock(CanonicalSubmissionService.class);
        QuestionResultRepository results = mock(QuestionResultRepository.class);
        when(service.get(eq(owner), eq(id))).thenReturn(submission(owner, id, SubmissionStatus.SUBMITTED));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ObjectiveResultController(service, results)).build();

        mvc.perform(get("/api/submissions/" + id + "/result").requestAttr(AuthInterceptor.PRINCIPAL_ATTRIBUTE,
                        new AuthPrincipal(owner, "learner@example.com", "Learner", UserRole.CUSTOMER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.questionResults").isEmpty())
                .andExpect(jsonPath("$.score").doesNotExist());
        verify(results, never()).findByOwnedSubmission(owner, id);
    }

    private static PracticeSubmission submission(UUID owner, UUID id, SubmissionStatus status) {
        Instant now = Instant.now();
        return new PracticeSubmission(id, owner, "READING", "set", "version", "set", 1, status,
                now, now, status == SubmissionStatus.GRADED ? now : null, null, 0, "start", "submit", "hash", false, now, now);
    }
}
