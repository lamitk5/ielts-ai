package com.ieltsaitutor.rag.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RightsStatus;

public interface RagDocumentRepository {
    void create(RagDocument document);
    Optional<RagDocument> findById(UUID id);
    List<RagDocument> listSummaries();
    void updateRightsStatus(UUID id, RightsStatus status, String rightsNote);
    void activate(UUID id);
    void deactivate(UUID id);
    void setCurrentVersion(UUID id, UUID versionId);
}
