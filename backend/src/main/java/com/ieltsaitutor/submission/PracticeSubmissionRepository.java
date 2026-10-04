package com.ieltsaitutor.submission;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

public interface PracticeSubmissionRepository {
    PracticeSubmission create(PracticeSubmission submission);

    Optional<PracticeSubmission> findById(UUID id);

    Optional<PracticeSubmission> findByOwnerAndId(UUID ownerId, UUID id);

    Optional<PracticeSubmission> findByOwnerAndStartIdempotencyKey(UUID ownerId, String key);

    Optional<PracticeSubmission> findByOwnerAndSubmitIdempotencyKey(UUID ownerId, String key);

    PracticeSubmission updateStatus(UUID id, SubmissionStatus status, Instant updatedAt);

    default PracticeSubmission markScored(UUID id, Instant scoredAt) {
        return updateStatus(id, SubmissionStatus.GRADED, scoredAt);
    }

    Optional<PracticeSubmission> updateAutosaveIfRevision(UUID id, long expectedRevision, Instant savedAt);

    Optional<PracticeSubmission> finalizeIfEditable(UUID id, String submitIdempotencyKey,
            String contentHash, java.time.Instant submittedAt);

    SubmissionHistoryPage findHistory(UUID ownerId, String skill, SubmissionStatus status, int page, int size);

    default SubmissionHistoryPage findAllHistory(AdminSubmissionQuery query) {
        return new SubmissionHistoryPage(java.util.List.of(), query.page(), query.size(), 0);
    }
}
