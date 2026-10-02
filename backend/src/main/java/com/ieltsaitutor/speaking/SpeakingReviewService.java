package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionStatus;

@Service
public class SpeakingReviewService {

    private final SpeakingReviewRepository reviewRepository;
    private final SpeakingSubmissionRepository speakingSubmissionRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;

    public SpeakingReviewService(
            SpeakingReviewRepository reviewRepository,
            SpeakingSubmissionRepository speakingSubmissionRepository,
            PracticeSubmissionRepository practiceSubmissionRepository) {
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
        this.speakingSubmissionRepository = Objects.requireNonNull(speakingSubmissionRepository, "speakingSubmissionRepository must not be null");
        this.practiceSubmissionRepository = Objects.requireNonNull(practiceSubmissionRepository, "practiceSubmissionRepository must not be null");
    }

    @Transactional
    public SpeakingReview submitReview(
            AuthPrincipal reviewer,
            UUID submissionId,
            Double overallBand,
            Double fluencyCoherence,
            Double lexicalResource,
            Double grammaticalRange,
            Double pronunciation,
            String feedback,
            String criteriaJson) {

        if (reviewer == null || reviewer.role() != UserRole.ADMIN) {
            throw new SecurityException("Chỉ quản trị viên/giám khảo mới có quyền chấm bài Speaking");
        }

        PracticeSubmission practiceSubmission = practiceSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Speaking submission không tồn tại: " + submissionId));

        SpeakingSubmission speakingSubmission = speakingSubmissionRepository.findBySubmissionId(submissionId)
                .orElse(null);

        int nextVersion = reviewRepository.getNextReviewVersion(submissionId);
        Instant now = Instant.now();

        SpeakingReview review = new SpeakingReview(
                UUID.randomUUID(),
                submissionId,
                reviewer.userId(),
                nextVersion,
                overallBand,
                fluencyCoherence,
                lexicalResource,
                grammaticalRange,
                pronunciation,
                feedback,
                criteriaJson,
                "COMPLETED",
                now,
                now
        );

        SpeakingReview saved = reviewRepository.save(review);
        if (speakingSubmission != null) {
            speakingSubmissionRepository.save(speakingSubmission.withStatus(SpeakingSubmissionState.GRADED));
        }
        practiceSubmissionRepository.updateStatus(submissionId, SubmissionStatus.GRADED, now);

        return saved;
    }

    public Optional<SpeakingReview> getLatestReview(AuthPrincipal requester, UUID submissionId) {
        PracticeSubmission submission = practiceSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Speaking submission không tồn tại: " + submissionId));

        if (requester == null || (requester.role() != UserRole.ADMIN && !submission.userId().equals(requester.userId()))) {
            throw new SecurityException("Không có quyền truy cập kết quả đánh giá Speaking");
        }

        return reviewRepository.findLatestBySubmission(submissionId);
    }

    public List<SpeakingReview> getReviewHistory(AuthPrincipal requester, UUID submissionId) {
        PracticeSubmission submission = practiceSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Speaking submission không tồn tại: " + submissionId));

        if (requester == null || (requester.role() != UserRole.ADMIN && !submission.userId().equals(requester.userId()))) {
            throw new SecurityException("Không có quyền truy cập lịch sử đánh giá Speaking");
        }

        return reviewRepository.findBySubmission(submissionId);
    }
}
