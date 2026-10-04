package com.ieltsaitutor.rag.embedding;

import com.ieltsaitutor.ai.provider.ProviderId;

import java.util.List;
import java.util.Optional;

/** Chooses a space without ever treating two provider spaces as interchangeable. */
public class EmbeddingSpaceSelector {
    private final List<EmbeddingProvider> providers;

    public EmbeddingSpaceSelector(List<EmbeddingProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public Optional<EmbeddingSpace> primarySpace() {
        return spaceFor(ProviderId.CLOUDFLARE).or(() -> spaceFor(ProviderId.GEMINI));
    }

    public Optional<EmbeddingSpace> spaceFor(ProviderId provider) {
        return providers.stream().filter(candidate -> candidate.providerId() == provider)
                .filter(EmbeddingProvider::isEmbeddingConfigured)
                .map(EmbeddingProvider::embeddingSpace)
                .findFirst();
    }

    public List<EmbeddingProvider> providersFor(EmbeddingSpace requested) {
        return providers.stream()
                .filter(EmbeddingProvider::isEmbeddingConfigured)
                .filter(provider -> provider.embeddingSpace().matches(requested))
                .toList();
    }
}
