package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RoadmapReconciliationService {
    private final LearningRoadmapPlanner planner;
    public RoadmapReconciliationService(LearningRoadmapPlanner planner) { this.planner = planner; }
    public LearningRoadmap reconcile(UUID userId, LearningRoadmap current, List<StudentLearningIssue> issues, Instant asOf) {
        LearningRoadmap next = planner.plan(userId, issues, asOf);
        if (current == null || current.status() != RoadmapStatus.ACTIVE) return next;
        return new LearningRoadmap(current.id(), userId, next.status(), current.version() + 1, asOf,
                next.items(), current.createdAt(), asOf);
    }
}
