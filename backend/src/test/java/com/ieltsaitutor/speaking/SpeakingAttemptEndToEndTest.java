package com.ieltsaitutor.speaking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionStatus;

class SpeakingAttemptEndToEndTest {

    @Test
    @DisplayName("Manual transcript uses one persistent attempt and never invents band")
    void manualTranscriptUsesOnePersistentAttemptAndNeverInventsBand() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        SpeakingRepository repository = mock(SpeakingRepository.class);
        SpeakingAttempt initial = new SpeakingAttempt(attemptId, userId, "speaking-p1-01", null, null,
                "IN_PROGRESS", null, Instant.now());
        when(repository.start(userId, "speaking-p1-01")).thenReturn(initial);
        when(repository.findByUserAndId(userId, attemptId)).thenReturn(Optional.of(initial));
        when(repository.saveDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.complete(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));

        SpeakingService service = new SpeakingService(new UnavailableSpeechToTextProvider(), repository);
        SpeakingAttempt started = service.startAttempt(userId, "speaking-p1-01");
        SpeakingAttempt saved = service.saveAttemptDraft(userId, attemptId, "I enjoy reading in the evening.");
        SpeakingAttempt completed = service.submitAttempt(userId, attemptId, saved.transcript());

        assertEquals(attemptId, started.id());
        assertEquals("I enjoy reading in the evening.", saved.transcript());
        assertEquals("INPUT_SAVED", completed.status());
        assertEquals(saved.transcript(), completed.transcript());
        assertNull(completed.overallBandEstimate());
        verify(repository).saveDraft(any());
        verify(repository).complete(any(), eq(completed));
    }

    @Test
    @DisplayName("Complete safe submission with audio, honest STT boundary, and audited review")
    void completeSafeSubmissionWithAudioAndAuditedReview() {
        UUID learnerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();

        AuthPrincipal admin = new AuthPrincipal(adminId, "examiner@test.com", "Examiner", UserRole.ADMIN);
        AuthPrincipal learner = new AuthPrincipal(learnerId, "student@test.com", "Student", UserRole.CUSTOMER);

        PracticeSubmission practiceSub = new PracticeSubmission(
                submissionId, learnerId, "SPEAKING", "speaking-p1-01", "v1", "set-1", 1,
                SubmissionStatus.SUBMITTED, Instant.now().minusSeconds(120), Instant.now(), null, Instant.now().minusSeconds(10),
                1L, "start-key", "submit-key", "hash", false, Instant.now().minusSeconds(120), Instant.now()
        );

        SpeakingSubmission speakingSub = new SpeakingSubmission(
                UUID.randomUUID(), submissionId, "speaking-p1-01", "v1", 0, 120,
                "audio-key-123.webm", "audio/webm", 4096L,
                "I believe technology enhances learning experiences.", "MANUAL",
                SpeakingSubmissionState.SUBMITTED, Instant.now().minusSeconds(120), Instant.now()
        );

        SpeakingReviewRepository reviewRepo = mock(SpeakingReviewRepository.class);
        SpeakingSubmissionRepository speakingRepo = mock(SpeakingSubmissionRepository.class);
        PracticeSubmissionRepository practiceRepo = mock(PracticeSubmissionRepository.class);

        when(practiceRepo.findById(submissionId)).thenReturn(Optional.of(practiceSub));
        when(speakingRepo.findBySubmissionId(submissionId)).thenReturn(Optional.of(speakingSub));
        when(reviewRepo.getNextReviewVersion(submissionId)).thenReturn(1);
        when(reviewRepo.save(any(SpeakingReview.class))).thenAnswer(inv -> inv.getArgument(0));

        SpeakingReviewService reviewService = new SpeakingReviewService(reviewRepo, speakingRepo, practiceRepo);

        SpeakingReview review = reviewService.submitReview(
                admin, submissionId, 7.5, 8.0, 7.5, 7.0, 7.5,
                "Good coherence, natural lexical use and clear pronunciation.",
                "{\"fluency\": 8.0, \"lexical\": 7.5}"
        );

        assertThat(review.overallBand()).isEqualTo(7.5);
        assertThat(review.reviewerUserId()).isEqualTo(adminId);
        assertThat(review.reviewVersion()).isEqualTo(1);

        verify(speakingRepo).save(argThat(s -> s.status() == SpeakingSubmissionState.GRADED));
        verify(practiceRepo).updateStatus(eq(submissionId), eq(SubmissionStatus.GRADED), any());
    }
}
