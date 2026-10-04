package com.ieltsaitutor.rag.ingestion;

import java.util.UUID;

import com.ieltsaitutor.rag.domain.IndexStatus;

public record IndexReceipt(UUID versionId, UUID jobId, int chunkCount, IndexStatus indexStatus) {}
