package com.ieltsaitutor.ai.attachment;

import java.util.List;
import java.util.UUID;

import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public interface TutorAttachmentChunkRepository {
    void replace(UUID attachmentId, List<TutorAttachmentChunk> chunks, List<List<Float>> embeddings,
            EmbeddingSpace space);

    List<RetrievedAttachmentChunk> findByAttachmentId(UUID attachmentId);

    List<RetrievedAttachmentChunk> findCandidates(UUID userId, UUID conversationId, List<UUID> attachmentIds,
            List<Float> queryEmbedding, EmbeddingSpace space, int limit);
}
