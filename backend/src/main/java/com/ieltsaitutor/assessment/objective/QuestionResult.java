package com.ieltsaitutor.assessment.objective;

import java.time.Instant;
import java.util.UUID;

public record QuestionResult(UUID id, UUID submissionId, UUID userId, String questionId, String questionType,
        String learnerAnswer, String normalizedLearnerAnswer, String correctAnswer, boolean correct,
        String evidenceReference, String explanation, String scoringPolicyVersion, Instant createdAt) {
    public QuestionResult {
        if (id == null || submissionId == null || userId == null) throw new IllegalArgumentException("Result identity is required");
        questionId = required(questionId, "questionId");
        questionType = required(questionType, "questionType");
        learnerAnswer = learnerAnswer == null ? "" : learnerAnswer;
        normalizedLearnerAnswer = normalizedLearnerAnswer == null ? "" : normalizedLearnerAnswer;
        correctAnswer = correctAnswer == null ? "" : correctAnswer;
        evidenceReference = evidenceReference == null ? "" : evidenceReference;
        explanation = explanation == null ? "" : explanation;
        scoringPolicyVersion = required(scoringPolicyVersion, "scoringPolicyVersion");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
}
