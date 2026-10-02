package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SpeakingReviewServiceTest {

    private SpeakingReviewRepository reviewRepository;
    private SpeakingSubmissionRepository speakingSubmissionRepository;
    private PracticeSubmissionRepository practiceSubmissionRepository;
    private SpeakingReviewService reviewService;

    private final UUID learnerId = UUID.randomUUID();
    private final UUID otherLearnerId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID submissionId = UUID.randomUUID();

    private final AuthPrincipal adminPrincipal = new AuthPrincipal(adminId, "admin@test.com", "Admin", UserRole.ADMIN);
    private final AuthPrincipal learnerPrincipal = new AuthPrincipal(learnerId, "learner@test.com", "Learner", UserRole.CUSTOMER);
    private final AuthPrincipal otherPrincipal = new AuthPrincipal(otherLearnerId, "other@test.com", "Other", UserRole.CUSTOMER);

    @BeforeEach
    void setUp() {
        reviewRepository = mock(SpeakingReviewRepository.class);
        speakingSubmissionRepository = mock(SpeakingSubmissionRepository.class);
        practiceSubmissionRepository = mock(PracticeSubmissionRepository.class);
        reviewService = new SpeakingReviewService(reviewRepository, speakingSubmissionRepository, practiceSubmissionRepository);
    }

    private PracticeSubmission samplePracticeSubmission() {
        return new PracticeSubmission(
                submissionId, learnerId, "SPEAKING", "speaking-p1-01", "v1", "set-1", 1,
                SubmissionStatus.SUBMITTED, Instant.now().minusSeconds(60), Instant.now(), null, Instant.now().minusSeconds(10),
                1L, "start-key", "submit-key", "hash", false, Instant.now().minusSeconds(60), Instant.now()
        );
    }

    private SpeakingSubmission sampleSpeakingSubmission() {
        return new SpeakingSubmission(
                UUID.randomUUID(), submissionId, "speaking-p1-01", "v1", 30, 120,
                "audio-key.webm", "audio/webm", 1024L, "Sample transcript", "MANUAL",
                SpeakingSubmissionState.SUBMITTED, Instant.now().minusSeconds(60), Instant.now()
        );
    }

    @Test
    @DisplayName("Admin can submit review, versioning increments and state becomes GRADED")
    void adminCanSubmitReview() {
        when(practiceSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(samplePracticeSubmission()));
        when(speakingSubmissionRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(sampleSpeakingSubmission()));
        when(reviewRepository.getNextReviewVersion(submissionId)).thenReturn(1);
        when(reviewRepository.save(any(SpeakingReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpeakingReview review = reviewService.submitReview(
                adminPrincipal, submissionId, 7.5, 7.0, 8.0, 7.5, 7.5,
                "Great intonation and coherent ideas.", "{\"fluency\": 7.0}"
        );

        assertThat(review.reviewVersion()).isEqualTo(1);
        assertThat(review.overallBand()).isEqualTo(7.5);
        assertThat(review.reviewerUserId()).isEqualTo(adminId);
        assertThat(review.status()).isEqualTo("COMPLETED");

        verify(speakingSubmissionRepository).save(argThat(s -> s.status() == SpeakingSubmissionState.GRADED));
        verify(practiceSubmissionRepository).updateStatus(eq(submissionId), eq(SubmissionStatus.GRADED), any());
    }

    @Test
    @DisplayName("Learner cannot submit review (throws SecurityException)")
    void learnerCannotSubmitReview() {
        assertThatThrownBy(() -> reviewService.submitReview(
                learnerPrincipal, submissionId, 7.0, 7.0, 7.0, 7.0, 7.0, "Self feedback", null
        )).isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("Learner can view their own review, other learner is rejected")
    void learnerCanViewOwnReview() {
        when(practiceSubmissionRepository.findById(submissionId)).thenReturn(Optional.of(samplePracticeSubmission()));
        SpeakingReview review = new SpeakingReview(
                UUID.randomUUID(), submissionId, adminId, 1, 7.0, 7.0, 7.0, 7.0, 7.0,
                "Good job", null, "COMPLETED", Instant.now(), Instant.now()
        );
        when(reviewRepository.findLatestBySubmission(submissionId)).thenReturn(Optional.of(review));

        Optional<SpeakingReview> result = reviewService.getLatestReview(learnerPrincipal, submissionId);
        assertThat(result).isPresent();
        assertThat(result.get().overallBand()).isEqualTo(7.0);

        assertThatThrownBy(() -> reviewService.getLatestReview(otherPrincipal, submissionId))
                .isInstanceOf(SecurityException.class);
    }
}
