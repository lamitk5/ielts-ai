package com.ieltsaitutor.rag.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.RagIngestionJob;

public interface RagIngestionJobRepository {
    void createJob(RagIngestionJob job);
    void updateStatus(RagIngestionJob job);
    Optional<RagIngestionJob> findById(UUID id);
    List<RagIngestionJob> listRecent(int limit);
}
