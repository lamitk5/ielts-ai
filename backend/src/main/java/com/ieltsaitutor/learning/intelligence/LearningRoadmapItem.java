package com.ieltsaitutor.learning.intelligence;

import java.util.Map;
import java.util.UUID;

public record LearningRoadmapItem(UUID id, UUID roadmapId, UUID userId, Skill skill, String learningObjective,
        String activityType, String targetErrorType, String targetQuestionType, int priority, String estimatedWorkload,
        RoadmapItemStatus status, String reasonCode, UUID evidenceIssueId, Map<String, Object> evidenceSnapshot) {
    public LearningRoadmapItem {
        if (priority < 1 || priority > 5) throw new IllegalArgumentException("roadmap priority must be 1..5");
        evidenceSnapshot = evidenceSnapshot == null ? Map.of() : Map.copyOf(evidenceSnapshot);
    }
}
