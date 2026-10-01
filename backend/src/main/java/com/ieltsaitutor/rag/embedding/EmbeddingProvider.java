package com.ieltsaitutor.rag.embedding;

import com.ieltsaitutor.ai.provider.ProviderId;

import java.util.List;

public interface EmbeddingProvider {
    EmbeddingResult embed(EmbeddingRequest request);
    List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests);

    default ProviderId providerId() { return ProviderId.GEMINI; }

    default boolean isEmbeddingConfigured() { return true; }

    default EmbeddingSpace embeddingSpace() {
        return new EmbeddingSpace(providerId(), "legacy", 768, "v1");
    }
}
