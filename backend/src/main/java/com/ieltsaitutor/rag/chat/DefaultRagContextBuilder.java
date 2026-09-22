package com.ieltsaitutor.rag.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.rag.retrieval.RetrievedChunk;

@Service
public class DefaultRagContextBuilder implements RagContextBuilder {
    private static final String OPEN = "<retrieved_evidence>\n";
    private static final String CLOSE = "</retrieved_evidence>";
    private final int maxCharacters;

    public DefaultRagContextBuilder() { this(12000); }
    public DefaultRagContextBuilder(int maxCharacters) {
        if (maxCharacters < 100) throw new IllegalArgumentException("Evidence budget is too small");
        this.maxCharacters = maxCharacters;
    }

    @Override
    public RagPromptContext build(List<RetrievedChunk> chunks) {
        StringBuilder evidence = new StringBuilder(OPEN);
        List<AiSource> sources = new ArrayList<>();
        if (chunks != null) {
            for (RetrievedChunk chunk : chunks) {
                if (chunk == null || chunk.content() == null || chunk.content().isBlank()) continue;
                String sourceId = chunk.sourceId() == null || chunk.sourceId().isBlank()
                        ? chunk.chunkId().toString() : chunk.sourceId();
                String block = sourceBlock(sources.size() + 1, sourceId, chunk);
                if (evidence.length() + block.length() + CLOSE.length() > maxCharacters) break;
                evidence.append(block);
                sources.add(new AiSource(sourceId, chunk.title(), chunk.section(), chunk.version(), chunk.page(),
                        chunk.chunkId() == null ? null : chunk.chunkId().toString()));
            }
        }
        evidence.append(CLOSE);
        return new RagPromptContext(evidence.toString(), sources);
    }

    private String sourceBlock(int number, String sourceId, RetrievedChunk chunk) {
        return "SOURCE " + number + "\n"
                + "source_id: " + sourceId + "\n"
                + "title: " + safe(chunk.title()) + "\n"
                + "version: " + safe(chunk.version()) + "\n"
                + "page: " + (chunk.page() == null ? "" : chunk.page()) + "\n"
                + "section: " + safe(chunk.section()) + "\n"
                + "chunk_id: " + (chunk.chunkId() == null ? "" : chunk.chunkId()) + "\n"
                + "content:\n" + chunk.content().trim() + "\n"
                + "END SOURCE\n";
    }

    private String safe(String value) { return value == null ? "" : value.replaceAll("[\\r\\n]", " "); }
}
