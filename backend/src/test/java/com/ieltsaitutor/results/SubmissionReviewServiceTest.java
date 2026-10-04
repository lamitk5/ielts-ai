package com.ieltsaitutor.results;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionStatus;

class SubmissionReviewServiceTest {
    @Test
    void persistsSeparateAppendOnlyHumanReviewForAdmin() {
        SubmissionReviewRepository reviews = mock(SubmissionReviewRepository.class);
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        UUID submissionId = UUID.randomUUID();
        PracticeSubmission submission = submission(submissionId, "WRITING");
        when(submissions.findById(submissionId)).thenReturn(Optional.of(submission));
        when(reviews.getNextReviewVersion(submissionId)).thenReturn(1);
        SubmissionReview saved = new SubmissionReview(UUID.randomUUID(), submissionId, UUID.randomUUID(), 1, 6.5, null, null,
                null, null, "Good", "{}", "COMPLETED", Instant.now(), Instant.now());
        when(reviews.save(org.mockito.ArgumentMatchers.any())).thenReturn(saved);
        SubmissionReviewService service = new SubmissionReviewService(reviews, submissions);

        SubmissionReview result = service.submit(new AuthPrincipal(saved.reviewerUserId(), "admin@example.com", "Admin", UserRole.ADMIN),
                submissionId, 6.5, null, null, null, null, "Good", "{}");

        assertSame(saved, result);
        verify(reviews).save(org.mockito.ArgumentMatchers.any(SubmissionReview.class));
        verify(submissions, never()).updateStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void learnerCannotSubmitReview() {
        SubmissionReviewService service = new SubmissionReviewService(mock(SubmissionReviewRepository.class), mock(PracticeSubmissionRepository.class));
        assertThrows(SecurityException.class, () -> service.submit(new AuthPrincipal(UUID.randomUUID(), "u@example.com", "User", UserRole.CUSTOMER),
                UUID.randomUUID(), 6.0, null, null, null, null, "", "{}"));
    }

    private static PracticeSubmission submission(UUID id, String skill) {
        Instant now = Instant.now();
        return new PracticeSubmission(id, UUID.randomUUID(), skill, "practice", "v1", "published", 1,
                SubmissionStatus.SUBMITTED, now, now, now, null, 0, "start", "submit", "hash", false, now, now);
    }
}
