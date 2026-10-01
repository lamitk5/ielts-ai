package com.ieltsaitutor.submission;

import java.util.Optional;
import java.util.UUID;

public interface PracticeSubmissionRepository {
    PracticeSubmission create(PracticeSubmission submission);

    Optional<PracticeSubmission> findById(UUID id);

    Optional<PracticeSubmission> findByOwnerAndId(UUID ownerId, UUID id);

    Optional<PracticeSubmission> findByOwnerAndStartIdempotencyKey(UUID ownerId, String key);

    Optional<PracticeSubmission> findByOwnerAndSubmitIdempotencyKey(UUID ownerId, String key);
}
