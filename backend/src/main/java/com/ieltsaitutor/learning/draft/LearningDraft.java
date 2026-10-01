package com.ieltsaitutor.learning.draft;

import java.time.Instant;
import java.util.UUID;

public record LearningDraft(
        UUID id,
        UUID userId,
        String skill,
        String referenceId,
        String contentSnapshot,
        long version,
        DraftStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt
) {
    public enum DraftStatus {
        ACTIVE,
        SUBMITTED,
        EXPIRED,
        DELETED
    }
}
