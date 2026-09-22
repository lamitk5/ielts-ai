package com.ieltsaitutor.rag.embedding;

public record EmbeddingRequest(String text, EmbeddingTask task) {
    public EmbeddingRequest {
        if (text == null || text.isBlank() || task == null) {
            throw new IllegalArgumentException("Embedding text and task are required");
        }
        text = text.trim();
    }
}
