package com.ieltsaitutor.rag.domain;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public record RagDocumentVersion(UUID id, UUID documentId, String version, String originalFilename, String mimeType,
        long fileSizeBytes, String checksum, String storagePath, ExtractionStatus extractionStatus,
        IndexStatus indexStatus, Instant approvedAt, Instant indexedAt, Instant createdAt, EmbeddingSpace embeddingSpace) {
    public RagDocumentVersion(UUID id, UUID documentId, String version, String originalFilename, String mimeType,
            long fileSizeBytes, String checksum, String storagePath, ExtractionStatus extractionStatus,
            IndexStatus indexStatus, Instant approvedAt, Instant indexedAt, Instant createdAt) {
        this(id, documentId, version, originalFilename, mimeType, fileSizeBytes, checksum, storagePath,
                extractionStatus, indexStatus, approvedAt, indexedAt, createdAt, null);
    }
}
