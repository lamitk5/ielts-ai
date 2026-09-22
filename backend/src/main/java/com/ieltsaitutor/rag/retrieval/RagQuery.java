package com.ieltsaitutor.rag.retrieval;

import com.ieltsaitutor.rag.domain.Skill;

public record RagQuery(String text, Skill skill, String language, String lessonId, String exerciseId,
        int topK, double minSimilarity) {
    public RagQuery {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("RAG query text is required");
        text = text.trim();
        if (topK < 0 || minSimilarity < 0 || minSimilarity > 1) {
            throw new IllegalArgumentException("RAG query limits are invalid");
        }
    }
}
