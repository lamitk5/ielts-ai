package com.ieltsaitutor.learning.intelligence;

import java.util.Optional;
import java.util.UUID;

public interface LearningEventRepository {
    Optional<LearningEvent> findByClientEvent(UUID userId, String clientEventId);
    Optional<LearningEvent> findBySourceReference(UUID userId, String sourceReference);
    void save(LearningEvent event);
}
