package com.ieltsaitutor.rag.embedding;

public record EmbeddingRequest(String text, EmbeddingTask task, EmbeddingSpace space) {
    public EmbeddingRequest(String text, EmbeddingTask task) { this(text, task, null); }

    public EmbeddingRequest {
        if (text == null || text.isBlank() || task == null) {
            throw new IllegalArgumentException("Embedding text and task are required");
        }
        text = text.trim();
    }

    public EmbeddingRequest withSpace(EmbeddingSpace selectedSpace) {
        return new EmbeddingRequest(text, task, selectedSpace);
    }
}
