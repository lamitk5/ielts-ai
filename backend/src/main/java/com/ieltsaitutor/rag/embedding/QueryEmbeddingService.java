package com.ieltsaitutor.rag.embedding;

public interface QueryEmbeddingService {
    EmbeddingVector embedQuery(String query);
}
