package com.ieltsaitutor.rag.domain;

import java.time.Instant;
import java.util.UUID;

public record RagIngestionJob(UUID id, UUID documentId, UUID documentVersionId, IngestionJobStatus status,
        String errorCode, String errorMessage, Instant startedAt, Instant finishedAt, Instant createdAt) {}
