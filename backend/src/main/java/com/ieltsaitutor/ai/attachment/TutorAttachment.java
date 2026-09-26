package com.ieltsaitutor.ai.attachment;

import java.time.Instant;
import java.util.UUID;

public record TutorAttachment(
        UUID id,
        UUID userId,
        String filename,
        String contentType,
        long sizeBytes,
        AttachmentStatus status,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public enum AttachmentStatus {
        SELECTED,
        UPLOADING,
        UPLOADED,
        PROCESSING,
        READY,
        FAILED,
        REMOVED,
        EXPIRED
    }
}
