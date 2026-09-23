package com.ieltsaitutor.rag.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public record RagChunk(UUID id, UUID documentVersionId, int chunkIndex, String content, Integer pageNumber,
        String sectionTitle, int tokenCount, List<Float> embedding, Map<String, Object> metadata, Instant createdAt,
        EmbeddingSpace embeddingSpace) {
    public RagChunk(UUID id, UUID documentVersionId, int chunkIndex, String content, Integer pageNumber,
            String sectionTitle, int tokenCount, List<Float> embedding, Map<String, Object> metadata, Instant createdAt) {
        this(id, documentVersionId, chunkIndex, content, pageNumber, sectionTitle, tokenCount, embedding, metadata,
                createdAt, null);
    }
}
