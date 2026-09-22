package com.ieltsaitutor.rag.domain;

import java.time.Instant;
import java.util.UUID;

public record RagDocumentVersion(UUID id, UUID documentId, String version, String originalFilename, String mimeType,
        long fileSizeBytes, String checksum, String storagePath, ExtractionStatus extractionStatus,
        IndexStatus indexStatus, Instant approvedAt, Instant indexedAt, Instant createdAt) {}
