package com.ieltsaitutor.learning.intelligence;

import java.util.UUID;
import java.util.function.Consumer;

public class RoadmapCompletionService {
    private final RoadmapRepository repository;
    private final Consumer<LearningRoadmapItem> completionEvent;

    public RoadmapCompletionService(RoadmapRepository repository, Consumer<LearningRoadmapItem> completionEvent) {
        this.repository = repository;
        this.completionEvent = completionEvent;
    }

    public LearningRoadmapItem complete(UUID userId, UUID itemId) {
        LearningRoadmapItem item = repository.findItem(userId, itemId)
                .orElseThrow(() -> new SecurityException("roadmap item is not owned by the user"));
        if (item.status() == RoadmapItemStatus.COMPLETED) return item;
        LearningRoadmapItem completed = repository.markCompleted(userId, itemId);
        completionEvent.accept(completed);
        return completed;
    }
}
