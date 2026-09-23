package com.ieltsaitutor.rag.embedding;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/** Embedding boundary that enforces exact-space selection for every request. */
@Component
@Primary
public class EmbeddingProviderRouter implements EmbeddingProvider {
    private final List<EmbeddingProvider> providers;
    private final EmbeddingSpaceSelector selector;

    public EmbeddingProviderRouter(CloudflareEmbeddingProvider cloudflare, GeminiEmbeddingProvider gemini) {
        this(List.of(cloudflare, gemini), new EmbeddingSpaceSelector(List.of(cloudflare, gemini)));
    }

    public EmbeddingProviderRouter(List<EmbeddingProvider> providers, EmbeddingSpaceSelector selector) {
        this.providers = List.copyOf(providers);
        this.selector = selector;
    }

    @Override
    public com.ieltsaitutor.ai.provider.ProviderId providerId() {
        return selector.primarySpace().map(space -> space.provider())
                .orElse(com.ieltsaitutor.ai.provider.ProviderId.CLOUDFLARE);
    }

    @Override
    public boolean isEmbeddingConfigured() { return selector.primarySpace().isPresent(); }

    @Override
    public EmbeddingSpace embeddingSpace() {
        return selector.primarySpace().orElseThrow(() -> unavailable(null));
    }

    @Override
    public EmbeddingResult embed(EmbeddingRequest request) {
        EmbeddingSpace requested = request.space() == null
                ? selector.primarySpace().orElseThrow(() -> unavailable(null))
                : request.space();
        if (requested.dimension() != 768) throw unavailable(requested);
        List<EmbeddingProvider> candidates = selector.providersFor(requested);
        if (candidates.isEmpty()) throw unavailable(requested);
        for (EmbeddingProvider provider : candidates) {
            EmbeddingResult result = provider.embed(request.withSpace(requested));
            if (result.space() == null || !result.space().matches(requested)) {
                throw new RagEmbeddingException("RAG_EMBEDDING_SPACE_MISMATCH", 502,
                        "Embedding provider trả về không gian vector không khớp.");
            }
            if (result.dimension() != 768) {
                throw new RagEmbeddingException("RAG_EMBEDDING_DIMENSION_MISMATCH", 502,
                        "Embedding dimension không khớp vector(768).");
            }
            return result;
        }
        throw unavailable(requested);
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests) {
        if (requests == null) throw new IllegalArgumentException("Embedding requests are required");
        return requests.stream().map(this::embed).toList();
    }

    private RagEmbeddingException unavailable(EmbeddingSpace requested) {
        String suffix = requested == null ? "" : " " + requested.key();
        return new RagEmbeddingException("RAG_EMBEDDING_UNAVAILABLE", 503,
                "Không có embedding provider tương thích và khả dụng cho không gian đã chọn." + suffix);
    }
}
