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
                learnerStatus(item.status()), item.startedAt(), item.submittedAt(), item.durationSeconds(),
                item.status() == SubmissionStatus.GRADED);
    }

    private static String learnerStatus(SubmissionStatus status) {
        return switch (status) {
            case DRAFT, IN_PROGRESS -> "IN_PROGRESS";
            case SUBMITTED, SCORING, AI_EVALUATING, PENDING_REVIEW -> "PROCESSING";
            case GRADED -> "COMPLETED";
            case FAILED -> "FAILED";
        };
    }
}
