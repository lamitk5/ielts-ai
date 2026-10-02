package com.ieltsaitutor.writing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WritingEvaluationRepository {
    WritingEvaluationResult save(WritingEvaluationResult evaluation);
    Optional<WritingEvaluationResult> findById(UUID id);
    List<WritingEvaluationResult> findByVersionId(UUID versionId);
    Optional<WritingEvaluationResult> findLatestByVersionId(UUID versionId);
    int getNextEvaluationVersion(UUID versionId);
}
