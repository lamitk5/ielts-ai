package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.UUID;

public record StudentSkillProfile(UUID userId, Skill skill, int attemptCount, int evaluatedItemCount,
        double accuracyRate, Double latestBandEstimate, EvidenceState evidenceState, String recentTrend,
        int strengthCount, int weaknessCount, Instant lastAttemptAt, Instant asOf) {
    public static StudentSkillProfile empty(UUID userId, Skill skill, Instant asOf) {
        return new StudentSkillProfile(userId, skill, 0, 0, 0d, null, EvidenceState.INSUFFICIENT_DATA,
                "INSUFFICIENT_DATA", 0, 0, null, asOf);
    }
}
