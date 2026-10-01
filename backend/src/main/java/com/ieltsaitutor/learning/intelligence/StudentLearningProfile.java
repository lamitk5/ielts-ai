package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.UUID;

public record StudentLearningProfile(UUID userId, int totalPracticeAttempts, int totalAnsweredQuestions,
        int activeRoadmapItemCount, Instant lastActivityAt, EvidenceState evidenceState, String overallTrend,
        double profileConfidence, Instant asOf, Instant updatedAt) {
    public static StudentLearningProfile empty(UUID userId, Instant asOf) {
        return new StudentLearningProfile(userId, 0, 0, 0, null, EvidenceState.INSUFFICIENT_DATA,
                "INSUFFICIENT_DATA", 0d, asOf, asOf);
    }
}
