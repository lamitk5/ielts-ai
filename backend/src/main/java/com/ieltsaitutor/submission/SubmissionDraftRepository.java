package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionDraftRepository {
    Optional<SubmissionDraftSnapshot> findBySubmissionId(UUID submissionId);

    Optional<SubmissionDraftSnapshot> saveIfRevision(UUID submissionId, UUID userId, Map<String, String> payload,
            long expectedRevision, String idempotencyKey, Instant updatedAt);
}
