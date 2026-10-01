package com.ieltsaitutor.writing;

import java.time.Instant;
import java.util.UUID;

public record WritingAttempt(UUID id, UUID userId, String taskId, String taskType, String status,
        String responseText, int wordCount, Instant createdAt, Instant submittedAt, WritingAssessment assessment) {
    public WritingAttempt withDraft(String text) {
        return new WritingAttempt(id, userId, taskId, taskType, status, text == null ? "" : text,
                countWords(text), createdAt, submittedAt, assessment);
    }

    public WritingAttempt withAssessment(WritingAssessment value) {
        return new WritingAttempt(id, userId, taskId, taskType, "FEEDBACK_READY", value.submittedText(),
                value.wordCount(), createdAt, Instant.now(), value);
    }

    private static int countWords(String value) {
        return value == null || value.isBlank() ? 0 : value.trim().split("\\s+").length;
    }
}
