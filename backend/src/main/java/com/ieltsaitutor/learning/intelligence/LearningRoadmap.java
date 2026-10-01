package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LearningRoadmap(UUID id, UUID userId, RoadmapStatus status, int version, Instant generatedFromAsOf,
        List<LearningRoadmapItem> items, Instant createdAt, Instant updatedAt) {
    public LearningRoadmap {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
