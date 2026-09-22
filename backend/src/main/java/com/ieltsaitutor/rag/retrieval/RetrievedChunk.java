package com.ieltsaitutor.rag.retrieval;

import java.util.UUID;

public record RetrievedChunk(UUID chunkId, UUID documentId, UUID versionId, String sourceId, String title,
        String version, Integer page, String section, String content, double similarity) {}
