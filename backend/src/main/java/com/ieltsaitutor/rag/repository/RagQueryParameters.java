package com.ieltsaitutor.rag.repository;

import java.util.List;

import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public record RagQueryParameters(List<Float> embedding, Skill skill, String language, int topK,
        double minSimilarity, EmbeddingSpace space) {
    public RagQueryParameters(List<Float> embedding, Skill skill, String language, int topK, double minSimilarity) {
        this(embedding, skill, language, topK, minSimilarity, null);
    }
}
