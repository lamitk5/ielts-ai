package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubmissionDraftSnapshot(
        UUID submissionId,
        UUID userId,
        Map<String, String> payload,
        long revision,
        String idempotencyKey,
        Instant updatedAt) {
    public SubmissionDraftSnapshot {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
