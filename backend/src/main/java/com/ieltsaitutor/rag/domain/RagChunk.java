package com.ieltsaitutor.rag.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RagChunk(UUID id, UUID documentVersionId, int chunkIndex, String content, Integer pageNumber,
        String sectionTitle, int tokenCount, List<Float> embedding, Map<String, Object> metadata, Instant createdAt) {}
