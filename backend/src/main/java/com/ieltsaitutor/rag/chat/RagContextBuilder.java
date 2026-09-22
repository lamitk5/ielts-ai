package com.ieltsaitutor.rag.chat;

import java.util.List;

import com.ieltsaitutor.rag.retrieval.RetrievedChunk;

public interface RagContextBuilder {
    RagPromptContext build(List<RetrievedChunk> chunks);
}
