package com.ieltsaitutor.rag.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.embedding.EmbeddingSpace;

public interface RagDocumentVersionRepository {
    void createVersion(RagDocumentVersion version);
    Optional<RagDocumentVersion> findById(UUID id);
    Optional<RagDocumentVersion> findCurrent(UUID documentId);
    Optional<RagDocumentVersion> findByChecksum(String checksum);
    void updateExtractionStatus(UUID id, ExtractionStatus status);
    void updateIndexStatus(UUID id, IndexStatus status);
    void setApprovedAt(UUID id, Instant approvedAt);
    void setIndexedAt(UUID id, Instant indexedAt);
    void setEmbeddingSpace(UUID id, EmbeddingSpace space);
}
