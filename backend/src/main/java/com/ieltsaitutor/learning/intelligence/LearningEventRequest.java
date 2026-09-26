package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LearningEventRequest(LearningEventType eventType, Skill skill, UUID sessionId, String practiceSetId,
        UUID attemptId, String questionId, UUID roadmapItemId, String sourceReference, Map<String, Object> payload,
        String clientEventId, Instant occurredAt) {}
