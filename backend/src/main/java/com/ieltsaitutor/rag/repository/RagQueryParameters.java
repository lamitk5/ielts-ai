package com.ieltsaitutor.rag.repository;

import java.util.List;

import com.ieltsaitutor.rag.domain.Skill;

public record RagQueryParameters(List<Float> embedding, Skill skill, String language, int topK,
        double minSimilarity) {}
