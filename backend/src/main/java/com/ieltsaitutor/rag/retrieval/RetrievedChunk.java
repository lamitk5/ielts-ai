package com.ieltsaitutor.rag.retrieval;

import java.util.UUID;

import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public record RetrievedChunk(UUID chunkId, UUID documentId, UUID versionId, String sourceId, String title,
        String version, Integer page, String section, String content, double similarity, EmbeddingSpace space) {
    public RetrievedChunk(UUID chunkId, UUID documentId, UUID versionId, String sourceId, String title,
            String version, Integer page, String section, String content, double similarity) {
        this(chunkId, documentId, versionId, sourceId, title, version, page, section, content, similarity, null);
    }
}
