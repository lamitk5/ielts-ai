package com.ieltsaitutor.learning.draft;

import java.util.Optional;
import java.util.UUID;

public interface LearningDraftRepository {
    Optional<LearningDraft> findActive(UUID userId, String skill, String referenceId);
    Optional<LearningDraft> findById(UUID id);
    LearningDraft save(LearningDraft draft);
    void delete(UUID id);
}
