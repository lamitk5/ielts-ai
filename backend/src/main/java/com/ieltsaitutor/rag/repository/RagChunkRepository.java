package com.ieltsaitutor.rag.repository;

import java.util.List;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.RagChunk;

public interface RagChunkRepository {
    void insertBatch(List<RagChunk> chunks);
    void deleteForVersion(UUID versionId);
    List<RagChunk> findGovernedCandidates(RagQueryParameters parameters);
}
