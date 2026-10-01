package com.ieltsaitutor.submission;

import java.util.Map;
import java.util.UUID;

public record SubmissionDraftCommand(
        UUID submissionId,
        UUID userId,
        Map<String, String> payload,
        long expectedRevision,
        String idempotencyKey) {
    public SubmissionDraftCommand {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision cannot be negative");
    }
}
