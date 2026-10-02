package com.ieltsaitutor.speaking;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpeakingReview(
        UUID id,
        UUID submissionId,
        UUID reviewerUserId,
        int reviewVersion,
        Double overallBand,
        Double fluencyCoherence,
        Double lexicalResource,
        Double grammaticalRange,
        Double pronunciation,
        String reviewerFeedback,
        String criteriaJson,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    public SpeakingReview {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(reviewerUserId, "reviewerUserId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (reviewVersion <= 0) throw new IllegalArgumentException("reviewVersion must be positive");
        validateScore("overallBand", overallBand);
        validateScore("fluencyCoherence", fluencyCoherence);
        validateScore("lexicalResource", lexicalResource);
        validateScore("grammaticalRange", grammaticalRange);
        validateScore("pronunciation", pronunciation);
        status = status != null && !status.isBlank() ? status.trim() : "COMPLETED";
    }

    private static void validateScore(String name, Double score) {
        if (score != null && (score < 0.0 || score > 9.0)) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 9.0");
        }
    }
}
