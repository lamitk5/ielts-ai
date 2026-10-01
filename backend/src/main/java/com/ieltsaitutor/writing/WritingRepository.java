package com.ieltsaitutor.writing;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface WritingRepository {
    void save(WritingAssessment assessment);
    default List<WritingAssessment> findByUser(UUID userId) { return List.of(); }
    default WritingAttempt start(UUID userId, String taskId) { throw new UnsupportedOperationException("Writing attempts are not configured"); }
    default Optional<WritingAttempt> find(UUID attemptId, UUID userId) { return Optional.empty(); }
    default WritingAttempt saveDraft(WritingAttempt attempt) { throw new UnsupportedOperationException("Writing attempts are not configured"); }
    default WritingAttempt complete(WritingAttempt attempt, WritingAssessment assessment) { throw new UnsupportedOperationException("Writing attempts are not configured"); }
}
