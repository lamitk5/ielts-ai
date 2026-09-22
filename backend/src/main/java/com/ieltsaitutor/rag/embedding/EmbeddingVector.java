package com.ieltsaitutor.rag.embedding;

import java.util.List;

public record EmbeddingVector(int dimension, List<Float> values) {
    public EmbeddingVector {
        if (dimension <= 0 || values == null || values.size() != dimension) {
            throw new IllegalArgumentException("Embedding vector dimension must match values");
        }
        values = List.copyOf(values);
    }
}
