package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record MockTestSection(
        UUID id,
        UUID sessionId,
        int sectionOrder,
        String skill,
        String practiceId,
        String practiceVersionId,
        String publishedSetId,
        UUID submissionId,
        int timeLimitSeconds,
        MockTestSectionStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public MockTestSection {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(skill, "skill must not be null");
        Objects.requireNonNull(practiceId, "practiceId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (sectionOrder < 0) throw new IllegalArgumentException("sectionOrder cannot be negative");
        practiceVersionId = practiceVersionId != null && !practiceVersionId.isBlank() ? practiceVersionId.trim() : "v1";
        publishedSetId = publishedSetId != null && !publishedSetId.isBlank() ? publishedSetId.trim() : "default-set";
    }

    public MockTestSection withSubmission(UUID newSubmissionId) {
        return new MockTestSection(
                id, sessionId, sectionOrder, skill, practiceId, practiceVersionId,
                publishedSetId, newSubmissionId, timeLimitSeconds, status, createdAt, Instant.now()
        );
    }

    public MockTestSection withStatus(MockTestSectionStatus newStatus) {
        return new MockTestSection(
                id, sessionId, sectionOrder, skill, practiceId, practiceVersionId,
                publishedSetId, submissionId, timeLimitSeconds, newStatus, createdAt, Instant.now()
        );
    }
}
