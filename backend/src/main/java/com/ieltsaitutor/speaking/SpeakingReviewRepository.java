package com.ieltsaitutor.speaking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpeakingReviewRepository {
    SpeakingReview save(SpeakingReview review);
    Optional<SpeakingReview> findLatestBySubmission(UUID submissionId);
    List<SpeakingReview> findBySubmission(UUID submissionId);
    int getNextReviewVersion(UUID submissionId);
}
