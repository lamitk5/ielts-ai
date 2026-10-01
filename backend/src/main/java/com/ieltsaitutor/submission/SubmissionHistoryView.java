package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.UUID;

public record SubmissionHistoryView(
        UUID id,
        String skill,
        String practiceId,
        String practiceVersionId,
        String status,
        Instant startedAt,
        Instant submittedAt,
        long durationSeconds,
        boolean scoreAvailable) {
    public static SubmissionHistoryView from(PracticeSubmission item) {
        return new SubmissionHistoryView(item.id(), item.skill(), item.practiceId(), item.practiceVersionId(),
                item.status().name(), item.startedAt(), item.submittedAt(), item.durationSeconds(),
                item.status() == SubmissionStatus.GRADED);
    }
}
