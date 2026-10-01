package com.ieltsaitutor.rag.retrieval;

import java.util.List;

import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public interface VectorRetrievalService {
    List<RetrievedChunk> search(RagQuery query);

    default List<RetrievedChunk> search(RagQuery query, EmbeddingSpace space) {
        return search(query.withSpace(space));
    }
}
