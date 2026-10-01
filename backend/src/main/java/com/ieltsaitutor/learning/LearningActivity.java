package com.ieltsaitutor.learning;

import java.time.Instant;
import java.util.UUID;

public record LearningActivity(UUID id, UUID userId, String skill, String activityType,
        String referenceId, Double score, Instant createdAt) {}
