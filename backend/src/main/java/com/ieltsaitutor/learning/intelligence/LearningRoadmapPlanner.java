package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class LearningRoadmapPlanner {
    public LearningRoadmap plan(UUID userId, List<StudentLearningIssue> issues, Instant asOf) {
        UUID roadmapId = UUID.randomUUID();
        List<LearningRoadmapItem> items = issues == null ? List.of() : issues.stream()
                .filter(issue -> issue.userId().equals(userId) && issue.kind() == IssueKind.WEAKNESS
                        && (issue.evidenceState() == EvidenceState.CONFIRMED || issue.evidenceState() == EvidenceState.EMERGING))
                .sorted(Comparator.comparingInt(StudentLearningIssue::occurrenceCount).reversed()
                        .thenComparing(StudentLearningIssue::category))
                .limit(5)
                .map(issue -> new LearningRoadmapItem(UUID.randomUUID(), roadmapId, userId, issue.skill(),
                        "Improve " + issue.category(), "TARGETED_PRACTICE", issue.category(), null,
                        Math.min(5, itemsPriority(issue)), "SHORT", RoadmapItemStatus.NOT_STARTED,
                        issue.evidenceCode() == null ? "RECURRENT_MISTAKE" : issue.evidenceCode(), issue.id(),
                        java.util.Map.of("occurrences", issue.occurrenceCount(), "attempts", issue.attemptCount())))
                .toList();
        return new LearningRoadmap(roadmapId, userId, items.isEmpty() ? RoadmapStatus.COMPLETED : RoadmapStatus.ACTIVE,
                1, asOf, items, asOf, asOf);
    }

    private int itemsPriority(StudentLearningIssue issue) { return Math.max(1, Math.min(5, issue.occurrenceCount())); }
}
