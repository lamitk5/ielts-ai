package com.ieltsaitutor.learning;

import java.time.Instant;
import java.util.UUID;

public record LearningAttempt(UUID id, UUID userId, String skill, int score, int total, Instant createdAt) {
    public LearningAttempt {
        if (score < 0 || total <= 0 || score > total) throw new IllegalArgumentException("Invalid attempt score");
        skill = skill.toLowerCase();
    }
}
