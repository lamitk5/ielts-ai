package com.ieltsaitutor.practice;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

public interface PracticeAttemptStore {
    void save(UUID userId, String skill, String setId, int score, int total, Map<String, String> answers);

    default Optional<PracticeAttemptSnapshot> findLatest(UUID userId, String skill, String setId) { return Optional.empty(); }
}
