package com.ieltsaitutor.ai.attachment;

import java.util.UUID;

import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public record RetrievedAttachmentChunk(UUID attachmentId, String filename, Integer pageNumber,
        String sectionLabel, int chunkIndex, String content, double similarity, EmbeddingSpace embeddingSpace) {}
