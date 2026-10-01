package com.ieltsaitutor.submission;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record PracticeSubmission(
        UUID id,
        UUID userId,
        String skill,
        String practiceId,
        String practiceVersionId,
        String publishedSetId,
        int publicationRevision,
        SubmissionStatus status,
        Instant startedAt,
        Instant lastSavedAt,
        Instant submittedAt,
        Instant scoredAt,
        long autosaveRevision,
        String startIdempotencyKey,
        String submitIdempotencyKey,
        String contentHash,
        boolean retryable,
        Instant createdAt,
        Instant updatedAt) {

    public PracticeSubmission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        skill = normalizedRequired(skill, "skill").toUpperCase(Locale.ROOT);
        practiceId = normalizedRequired(practiceId, "practiceId");
        practiceVersionId = normalizedRequired(practiceVersionId, "practiceVersionId");
        publishedSetId = normalizedRequired(publishedSetId, "publishedSetId");
        if (publicationRevision < 1) throw new IllegalArgumentException("publicationRevision must be positive");
        if (autosaveRevision < 0) throw new IllegalArgumentException("autosaveRevision cannot be negative");
        startIdempotencyKey = normalizeOptional(startIdempotencyKey);
        submitIdempotencyKey = normalizeOptional(submitIdempotencyKey);
        contentHash = normalizeOptional(contentHash);
    }

    public long durationSeconds() {
        Instant end = submittedAt == null ? lastSavedAt : submittedAt;
        if (end == null || end.isBefore(startedAt)) return 0;
        return Duration.between(startedAt, end).getSeconds();
    }

    public boolean editable() {
        return status == SubmissionStatus.DRAFT || status == SubmissionStatus.IN_PROGRESS;
    }

    private static String normalizedRequired(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
