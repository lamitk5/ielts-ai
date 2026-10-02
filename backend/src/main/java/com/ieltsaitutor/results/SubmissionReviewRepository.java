package com.ieltsaitutor.results;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionReviewRepository {
    SubmissionReview save(SubmissionReview review);
    Optional<SubmissionReview> findLatestBySubmission(UUID submissionId);
    List<SubmissionReview> findBySubmission(UUID submissionId);
    int getNextReviewVersion(UUID submissionId);
}
