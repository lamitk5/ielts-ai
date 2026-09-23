package com.ieltsaitutor.rag.embedding;

import org.springframework.stereotype.Service;

@Service
public class DefaultQueryEmbeddingService implements QueryEmbeddingService {
    private final EmbeddingProvider provider;

    public DefaultQueryEmbeddingService(EmbeddingProvider provider) {
        this.provider = provider;
    }

    @Override
    public EmbeddingVector embedQuery(String query) {
        EmbeddingResult result = provider.embed(new EmbeddingRequest(query, EmbeddingTask.QUERY));
        return new EmbeddingVector(result.dimension(), result.values(), result.space());
    }
}
