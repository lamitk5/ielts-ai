package com.ieltsaitutor.results;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionConflictException;

@Service
public class SubmissionReviewService {
    private final SubmissionReviewRepository reviews;
    private final PracticeSubmissionRepository submissions;

    public SubmissionReviewService(SubmissionReviewRepository reviews, PracticeSubmissionRepository submissions) {
        this.reviews = reviews;
        this.submissions = submissions;
    }

    @Transactional
    public SubmissionReview submit(AuthPrincipal reviewer, UUID submissionId, Double overallBand,
            Double fluencyCoherence, Double lexicalResource, Double grammaticalRange, Double pronunciation,
            String feedback, String criteriaJson) {
        requireAdmin(reviewer);
        PracticeSubmission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission not found"));
        if (!"WRITING".equals(submission.skill()) && !"SPEAKING".equals(submission.skill())) {
            throw new SubmissionConflictException("Human review is only available for Writing and Speaking");
        }
        Instant now = Instant.now();
        SubmissionReview review = new SubmissionReview(UUID.randomUUID(), submissionId, reviewer.userId(),
                reviews.getNextReviewVersion(submissionId), overallBand, fluencyCoherence, lexicalResource,
                grammaticalRange, pronunciation, feedback, criteriaJson, "COMPLETED", now, now);
        return reviews.save(review);
    }

    public SubmissionReview latestForLearner(AuthPrincipal requester, UUID submissionId) {
        PracticeSubmission submission = ownedOrAdmin(requester, submissionId);
        return reviews.findLatestBySubmission(submission.id()).orElse(null);
    }

    public List<SubmissionReview> historyForLearner(AuthPrincipal requester, UUID submissionId) {
        PracticeSubmission submission = ownedOrAdmin(requester, submissionId);
        return reviews.findBySubmission(submission.id());
    }

    private PracticeSubmission ownedOrAdmin(AuthPrincipal requester, UUID submissionId) {
        if (requester == null) throw new SecurityException("Authentication is required");
        if (requester.role() == UserRole.ADMIN) {
            return submissions.findById(submissionId).orElseThrow(() -> new SubmissionConflictException("Submission not found"));
        }
        return submissions.findByOwnerAndId(requester.userId(), submissionId)
                .orElseThrow(() -> new SecurityException("Submission not found or access denied"));
    }

    private void requireAdmin(AuthPrincipal principal) {
        if (principal == null || principal.role() != UserRole.ADMIN) {
            throw new SecurityException("Quyền quản trị viên là bắt buộc.");
        }
    }
}
