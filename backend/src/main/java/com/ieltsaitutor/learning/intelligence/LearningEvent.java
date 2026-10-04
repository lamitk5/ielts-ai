package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LearningEvent(UUID id, UUID userId, LearningEventType eventType, Skill skill, UUID sessionId,
        String practiceSetId, UUID attemptId, String questionId, UUID roadmapItemId, String sourceReference,
        Map<String, Object> payload, String clientEventId, Instant occurredAt, Instant recordedAt) {
    public LearningEvent {
        if (id == null || userId == null || eventType == null || skill == null) throw new IllegalArgumentException("event identity is required");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        if (payload.size() > 32) throw new IllegalArgumentException("event payload is too large");
    }
}
