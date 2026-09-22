package com.ieltsaitutor.rag.retrieval;

import java.util.List;

public interface VectorRetrievalService {
    List<RetrievedChunk> search(RagQuery query);
}
