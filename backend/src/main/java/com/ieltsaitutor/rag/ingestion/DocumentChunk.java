package com.ieltsaitutor.rag.ingestion;

import java.util.Map;

public record DocumentChunk(int chunkIndex, String content, Integer pageNumber, String sectionTitle,
        int tokenCount, Map<String, Object> metadata) {
    public DocumentChunk {
        content = content == null ? "" : content.trim();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
