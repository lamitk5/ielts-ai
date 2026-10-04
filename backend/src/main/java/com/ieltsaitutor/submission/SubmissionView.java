package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.UUID;

public record SubmissionView(
        UUID id,
        String skill,
        String practiceId,
        String practiceVersionId,
        String publishedSetId,
        int publicationRevision,
        String status,
        Instant startedAt,
        Instant lastSavedAt,
        Instant submittedAt,
        long autosaveRevision,
        long durationSeconds,
        boolean retryable) {
    public static SubmissionView from(PracticeSubmission item) {
        return new SubmissionView(item.id(), item.skill(), item.practiceId(), item.practiceVersionId(),
                item.publishedSetId(), item.publicationRevision(), item.status().name(), item.startedAt(),
                item.lastSavedAt(), item.submittedAt(), item.autosaveRevision(), item.durationSeconds(), item.retryable());
    }
}
