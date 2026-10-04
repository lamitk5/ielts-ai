package com.ieltsaitutor.admin.submission;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.submission.PracticeSubmission;

public record AdminSubmissionView(UUID id, UUID userId, String skill, String practiceId, String practiceVersionId,
        String status, Instant submittedAt, long durationSeconds, Instant createdAt) {
    public static AdminSubmissionView from(PracticeSubmission item) {
        return new AdminSubmissionView(item.id(), item.userId(), item.skill(), item.practiceId(), item.practiceVersionId(),
                item.status().name(), item.submittedAt(), item.durationSeconds(), item.createdAt());
    }
}
