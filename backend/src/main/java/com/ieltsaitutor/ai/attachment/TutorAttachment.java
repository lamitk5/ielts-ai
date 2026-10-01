package com.ieltsaitutor.ai.attachment;

import java.time.Instant;
import java.util.UUID;

public record TutorAttachment(
        UUID id,
        UUID userId,
        UUID conversationId,
        String filename,
        String sanitizedFilename,
        String contentType,
        AttachmentKind kind,
        long sizeBytes,
        String sha256,
        String storageKey,
        AttachmentStatus status,
        String processingErrorCode,
        Instant processingStartedAt,
        int processingAttempts,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        String requestId,
        String capability
) {
    public TutorAttachment(
            UUID id,
            UUID userId,
            UUID conversationId,
            String filename,
            String sanitizedFilename,
            String contentType,
            AttachmentKind kind,
            long sizeBytes,
            String sha256,
            String storageKey,
            AttachmentStatus status,
            String processingErrorCode,
            Instant processingStartedAt,
            int processingAttempts,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt
    ) {
        this(id, userId, conversationId, filename, sanitizedFilename, contentType, kind, sizeBytes, sha256,
                storageKey, status, processingErrorCode, processingStartedAt, processingAttempts, createdAt,
                updatedAt, expiresAt, null, null);
    }

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
        this(id, userId, null, filename, filename, contentType, kindFor(contentType, filename), sizeBytes, null,
                null, status, errorMessage, null, 0, createdAt, updatedAt, null, null, null);
    }

    public TutorAttachment(
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
        this(id, userId, null, filename, filename, contentType, kindFor(contentType, filename), sizeBytes, null,
                null, status, errorMessage, null, 0, createdAt, updatedAt, null, requestId, capability);
    }

    public String errorMessage() {
        return processingErrorCode;
    }

    private static AttachmentKind kindFor(String contentType, String filename) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("image/") || TutorAttachmentContract.isImage(TutorAttachmentContract.extensionOf(filename))
                ? AttachmentKind.IMAGE : AttachmentKind.DOCUMENT;
    }

    public enum AttachmentStatus {
        SELECTED,
        UPLOADING,
        UPLOADED,
        STORED,
        PROCESSING,
        READY,
        IMAGE_READY,
        FAILED,
        REMOVED,
        EXPIRED
    }
}
