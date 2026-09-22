package com.ieltsaitutor.rag.embedding;

import java.util.List;

public interface EmbeddingProvider {
    EmbeddingResult embed(EmbeddingRequest request);
    List<EmbeddingResult> embedBatch(List<EmbeddingRequest> requests);
}
