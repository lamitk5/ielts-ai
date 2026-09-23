package com.ieltsaitutor.rag.embedding;

import java.util.List;

public record EmbeddingVector(int dimension, List<Float> values, EmbeddingSpace space) {
    public EmbeddingVector(int dimension, List<Float> values) { this(dimension, values, null); }

    public EmbeddingVector {
        if (dimension <= 0 || values == null || values.size() != dimension) {
            throw new IllegalArgumentException("Embedding vector dimension must match values");
        }
        values = List.copyOf(values);
    }
}
