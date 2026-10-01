package com.ieltsaitutor.submission;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface PracticeSubmissionRepository {
    PracticeSubmission create(PracticeSubmission submission);

    Optional<PracticeSubmission> findById(UUID id);

    Optional<PracticeSubmission> findByOwnerAndId(UUID ownerId, UUID id);

    Optional<PracticeSubmission> findByOwnerAndStartIdempotencyKey(UUID ownerId, String key);

    Optional<PracticeSubmission> findByOwnerAndSubmitIdempotencyKey(UUID ownerId, String key);

    PracticeSubmission updateStatus(UUID id, SubmissionStatus status, Instant updatedAt);

    Optional<PracticeSubmission> updateAutosaveIfRevision(UUID id, long expectedRevision, Instant savedAt);

    Optional<PracticeSubmission> finalizeIfEditable(UUID id, String submitIdempotencyKey,
            String contentHash, java.time.Instant submittedAt);
}
