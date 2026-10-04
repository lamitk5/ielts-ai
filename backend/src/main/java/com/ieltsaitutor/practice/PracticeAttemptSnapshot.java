package com.ieltsaitutor.practice;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PracticeAttemptSnapshot(UUID id, UUID userId, String skill, String setId, int score, int total,
        Map<String, String> answers, Instant createdAt) {
    public PracticeAttemptSnapshot {
        answers = answers == null ? Map.of() : Map.copyOf(answers);
    }
}
