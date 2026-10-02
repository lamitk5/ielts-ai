package com.ieltsaitutor.tutor.context;

import java.util.UUID;

/** Stable references only; client-provided answers, scores and bands are intentionally absent. */
public record TutorContextRequest(String skill, String setId, String questionId, UUID attemptId,
        String taskId, String promptId, UUID resultId) {
    public TutorContextRequest {
        skill = normalize(skill);
        setId = normalizeNullable(setId);
        questionId = normalizeNullable(questionId);
        taskId = normalizeNullable(taskId);
        promptId = normalizeNullable(promptId);
    }

    public TutorContextRequest(String skill, String setId, String questionId, UUID attemptId,
            String taskId, String promptId) {
        this(skill, setId, questionId, attemptId, taskId, promptId, null);
    }

    private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(); }
    private static String normalizeNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
