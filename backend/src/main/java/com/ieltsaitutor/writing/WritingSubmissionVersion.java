package com.ieltsaitutor.writing;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WritingSubmissionVersion(
        UUID id,
        UUID submissionId,
        int versionNumber,
        UUID parentVersionId,
        String responseText,
        int wordCount,
        String contentHash,
        Instant createdAt) {

    public WritingSubmissionVersion {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (versionNumber < 1) {
            throw new IllegalArgumentException("versionNumber must be positive");
        }
        if (responseText == null) {
            throw new IllegalArgumentException("responseText must not be null");
        }
        if (wordCount < 0) {
            throw new IllegalArgumentException("wordCount cannot be negative");
        }
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("contentHash must not be blank");
        }
    }
}
