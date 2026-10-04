package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubmissionAnswerSnapshot(
        UUID submissionId,
        UUID userId,
        Map<String, String> answers,
        String contentHash,
        Instant submittedAt) {
    public SubmissionAnswerSnapshot {
        answers = answers == null ? Map.of() : Map.copyOf(answers);
    }
}
