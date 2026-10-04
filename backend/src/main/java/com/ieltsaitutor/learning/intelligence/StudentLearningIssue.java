package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.UUID;

public record StudentLearningIssue(UUID id, UUID userId, IssueKind kind, Skill skill, String category,
        EvidenceState evidenceState, IssueStatus status, double confidence, int occurrenceCount,
        int attemptCount, Instant lastObservedAt, String evidenceCode) {}
