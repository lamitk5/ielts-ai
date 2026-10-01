package com.ieltsaitutor.rag.retrieval;

import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public record RagQuery(String text, Skill skill, String language, String lessonId, String exerciseId,
        int topK, double minSimilarity, EmbeddingSpace space) {
    public RagQuery(String text, Skill skill, String language, String lessonId, String exerciseId,
            int topK, double minSimilarity) {
        this(text, skill, language, lessonId, exerciseId, topK, minSimilarity, null);
    }

    public RagQuery {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("RAG query text is required");
        text = text.trim();
        if (topK < 0 || minSimilarity < 0 || minSimilarity > 1) {
            throw new IllegalArgumentException("RAG query limits are invalid");
        }
    }

    public RagQuery withSpace(EmbeddingSpace selectedSpace) {
        return new RagQuery(text, skill, language, lessonId, exerciseId, topK, minSimilarity, selectedSpace);
    }
}
