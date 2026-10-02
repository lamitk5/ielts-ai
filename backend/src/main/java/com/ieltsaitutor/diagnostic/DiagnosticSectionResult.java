package com.ieltsaitutor.diagnostic;

import com.ieltsaitutor.learning.intelligence.Skill;
import java.time.Instant;
import java.util.UUID;

public record DiagnosticSectionResult(UUID id, UUID sessionId, UUID userId, Skill skill,
        DiagnosticSectionState state, Integer score, Integer total, Double estimatedBand,
        DiagnosticConfidence confidence, UUID sourceSubmissionId, String sourceReference,
        String availabilityMessage, Instant createdAt) {
    public DiagnosticSectionResult {
        if (id == null || sessionId == null || userId == null || skill == null || state == null || confidence == null)
            throw new IllegalArgumentException("diagnostic section identity is required");
        if (score != null && (total == null || total <= 0 || score < 0 || score > total))
            throw new IllegalArgumentException("diagnostic section score is invalid");
        if (createdAt == null) createdAt = Instant.now();
    }

    public static DiagnosticSectionResult insufficient(UUID sessionId, UUID userId, Skill skill, String message, Instant now) {
        return new DiagnosticSectionResult(UUID.randomUUID(), sessionId, userId, skill,
                DiagnosticSectionState.INSUFFICIENT_EVIDENCE, null, null, null,
                DiagnosticConfidence.INSUFFICIENT_DATA, null, null, message, now);
    }
}
