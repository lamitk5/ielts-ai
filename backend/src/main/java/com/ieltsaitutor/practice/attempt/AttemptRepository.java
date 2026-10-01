package com.ieltsaitutor.practice.attempt;

import java.util.Optional;
import java.util.Map;
import java.util.UUID;

public interface AttemptRepository {
    PracticeAttempt create(UUID userId, String practiceId, String practiceVersion, String skill, String idempotencyKey);
    Optional<PracticeAttempt> findById(UUID id);
    Optional<PracticeAttempt> findByUserAndIdempotencyKey(UUID userId, String idempotencyKey);
    PracticeAttempt saveAnswers(PracticeAttempt attempt, Map<String, String> answers);
    default PracticeAttempt saveSubmitted(PracticeAttempt attempt, Map<String, String> answers) {
        throw new UnsupportedOperationException("Legacy repository does not support canonical submission");
    }
    PracticeAttempt saveResult(PracticeAttempt attempt, Map<String, String> answers, int score, int total, String resultPayload);
}
