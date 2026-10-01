package com.ieltsaitutor.rag.embedding;

import com.ieltsaitutor.ai.provider.ProviderId;

/** Immutable identity for one physically comparable embedding vector space. */
public record EmbeddingSpace(ProviderId provider, String model, int dimension, String version) {
    public EmbeddingSpace {
        if (provider == null || model == null || model.isBlank() || dimension != 768
                || version == null || version.isBlank()) {
            throw new IllegalArgumentException("Embedding space must use a provider, model, version and vector(768)");
        }
        model = model.trim();
        version = version.trim();
    }

    public String key() { return provider.name() + ":" + model + ":" + dimension + ":" + version; }

    public boolean matches(EmbeddingSpace other) { return equals(other); }
}
