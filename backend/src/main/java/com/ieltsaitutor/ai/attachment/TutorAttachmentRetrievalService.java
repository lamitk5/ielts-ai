package com.ieltsaitutor.ai.attachment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.rag.embedding.EmbeddingSpace;
import com.ieltsaitutor.rag.embedding.EmbeddingVector;
import com.ieltsaitutor.rag.embedding.QueryEmbeddingService;

@Service
public class TutorAttachmentRetrievalService {
    private static final int TOP_K = 8;
    private static final int MAX_PER_FILE = 4;
    private static final int CANDIDATE_LIMIT = TOP_K * MAX_PER_FILE;
    private final TutorAttachmentChunkRepository repository;
    private final QueryEmbeddingService queryEmbedding;
    private final EmbeddingSpace configuredSpace;

    @Autowired
    public TutorAttachmentRetrievalService(TutorAttachmentChunkRepository repository, QueryEmbeddingService queryEmbedding,
            com.ieltsaitutor.rag.embedding.EmbeddingProvider provider) {
        this(repository, queryEmbedding, configuredSpace(provider));
    }

    public TutorAttachmentRetrievalService(TutorAttachmentChunkRepository repository, QueryEmbeddingService queryEmbedding,
            EmbeddingSpace configuredSpace) {
        this.repository = repository;
        this.queryEmbedding = queryEmbedding;
        this.configuredSpace = configuredSpace;
    }

    private static EmbeddingSpace configuredSpace(com.ieltsaitutor.rag.embedding.EmbeddingProvider provider) {
        return provider != null && provider.isEmbeddingConfigured() ? provider.embeddingSpace() : null;
    }

    public List<RetrievedAttachmentChunk> retrieve(UUID userId, UUID conversationId, List<UUID> attachmentIds,
            String query) {
        if (attachmentIds == null || attachmentIds.isEmpty()) return List.of();
        EmbeddingVector embedding = queryEmbedding.embedQuery(query);
        EmbeddingSpace space = embedding.space() != null ? embedding.space() : configuredSpace;
        if (space == null || space.dimension() != 768 || embedding.dimension() != 768
                || (embedding.space() != null && configuredSpace != null && !embedding.space().matches(configuredSpace))) {
            throw new IllegalStateException("RAG_EMBEDDING_SPACE_MISMATCH");
        }
        List<RetrievedAttachmentChunk> candidates = repository.findCandidates(userId, conversationId, attachmentIds,
                embedding.values(), space, CANDIDATE_LIMIT);
        return diversify(candidates);
    }

    private List<RetrievedAttachmentChunk> diversify(List<RetrievedAttachmentChunk> candidates) {
        Map<UUID, List<RetrievedAttachmentChunk>> grouped = new LinkedHashMap<>();
        for (RetrievedAttachmentChunk candidate : candidates) {
            if (candidate.embeddingSpace() != null && configuredSpace != null
                    && !candidate.embeddingSpace().matches(configuredSpace)) continue;
            grouped.computeIfAbsent(candidate.attachmentId(), ignored -> new ArrayList<>()).add(candidate);
        }
        List<RetrievedAttachmentChunk> selected = new ArrayList<>();
        int round = 0;
        while (selected.size() < TOP_K && !grouped.isEmpty()) {
            boolean added = false;
            for (List<RetrievedAttachmentChunk> fileChunks : grouped.values()) {
                if (round < fileChunks.size() && round < MAX_PER_FILE) {
                    selected.add(fileChunks.get(round));
                    added = true;
                    if (selected.size() == TOP_K) break;
                }
            }
            if (!added) break;
            round++;
        }
        return List.copyOf(selected);
    }
}
