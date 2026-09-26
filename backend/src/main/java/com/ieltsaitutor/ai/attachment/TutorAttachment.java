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
        Instant updatedAt,
        String requestId,
        String capability
) {
    public TutorAttachment(
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
        this(id, userId, filename, contentType, sizeBytes, status, errorMessage, createdAt, updatedAt, null, null);
    }

    public enum AttachmentStatus {
        SELECTED,
        UPLOADING,
        UPLOADED,
        PROCESSING,
        READY,
        IMAGE_READY,
        FAILED,
        REMOVED,
        EXPIRED
    }
}
