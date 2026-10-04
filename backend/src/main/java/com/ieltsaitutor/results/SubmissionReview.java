package com.ieltsaitutor.results;

import java.time.Instant;
import java.util.UUID;

public record SubmissionReview(
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
    public SubmissionReview {
        if (reviewVersion < 1) throw new IllegalArgumentException("reviewVersion must be positive");
        if (overallBand != null && (overallBand < 0 || overallBand > 9)) throw new IllegalArgumentException("overallBand must be 0..9");
        if (fluencyCoherence != null && (fluencyCoherence < 0 || fluencyCoherence > 9)) throw new IllegalArgumentException("fluencyCoherence must be 0..9");
        if (lexicalResource != null && (lexicalResource < 0 || lexicalResource > 9)) throw new IllegalArgumentException("lexicalResource must be 0..9");
        if (grammaticalRange != null && (grammaticalRange < 0 || grammaticalRange > 9)) throw new IllegalArgumentException("grammaticalRange must be 0..9");
        if (pronunciation != null && (pronunciation < 0 || pronunciation > 9)) throw new IllegalArgumentException("pronunciation must be 0..9");
        status = status == null || status.isBlank() ? "COMPLETED" : status.trim();
    }
}
