package com.ieltsaitutor.rag.embedding;

import java.util.List;

public record EmbeddingResult(String model, int dimension, List<Float> values) {
    public EmbeddingResult {
        if (model == null || model.isBlank() || dimension <= 0 || values == null || values.size() != dimension) {
            throw new IllegalArgumentException("Embedding result has an invalid dimension");
        }
        values = List.copyOf(values);
    }
}
