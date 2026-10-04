package com.ieltsaitutor.learning.intelligence;

import java.util.Optional;
import java.util.UUID;

public interface RoadmapRepository {
    Optional<LearningRoadmapItem> findItem(UUID userId, UUID itemId);
    LearningRoadmapItem markCompleted(UUID userId, UUID itemId);
}
