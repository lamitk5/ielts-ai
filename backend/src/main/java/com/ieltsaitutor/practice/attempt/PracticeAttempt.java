package com.ieltsaitutor.practice.attempt;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PracticeAttempt(
        UUID id,
        UUID userId,
        String practiceId,
        String practiceVersion,
        String skill,
        AttemptStatus status,
        Map<String, String> answers,
        Integer score,
        Integer total,
        Instant startedAt,
        Instant submittedAt,
        String resultPayload,
        String idempotencyKey) {

    public PracticeAttempt {
        answers = answers == null ? Map.of() : Map.copyOf(answers);
        skill = skill == null ? "" : skill.trim().toLowerCase();
        practiceId = practiceId == null ? "" : practiceId;
        practiceVersion = practiceVersion == null ? "" : practiceVersion;
        resultPayload = resultPayload == null ? "{}" : resultPayload;
    }

    public PracticeAttempt withAnswers(Map<String, String> updatedAnswers) {
        return new PracticeAttempt(id, userId, practiceId, practiceVersion, skill, AttemptStatus.IN_PROGRESS,
                updatedAnswers, score, total, startedAt, submittedAt, resultPayload, idempotencyKey);
    }

    public PracticeAttempt withResult(Map<String, String> updatedAnswers, int updatedScore, int updatedTotal, String updatedResultPayload) {
        return new PracticeAttempt(id, userId, practiceId, practiceVersion, skill, AttemptStatus.FEEDBACK_READY,
                updatedAnswers, updatedScore, updatedTotal, startedAt, Instant.now(), updatedResultPayload, idempotencyKey);
    }
}
