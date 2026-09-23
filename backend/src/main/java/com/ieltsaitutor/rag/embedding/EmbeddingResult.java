package com.ieltsaitutor.rag.embedding;

import java.util.List;

public record EmbeddingResult(String model, int dimension, List<Float> values, EmbeddingSpace space) {
    public EmbeddingResult(String model, int dimension, List<Float> values) {
        this(model, dimension, values, null);
    }

    public EmbeddingResult {
        if (model == null || model.isBlank() || dimension <= 0 || values == null || values.size() != dimension) {
            throw new IllegalArgumentException("Embedding result has an invalid dimension");
        }
        if (space != null && (!space.model().equals(model) || space.dimension() != dimension)) {
            throw new IllegalArgumentException("Embedding result does not match its embedding space");
        }
        values = List.copyOf(values);
    }
}
