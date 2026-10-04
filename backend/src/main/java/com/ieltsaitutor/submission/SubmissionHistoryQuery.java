package com.ieltsaitutor.submission;

public record SubmissionHistoryQuery(String skill, SubmissionStatus status, int page, int size) {
    public SubmissionHistoryQuery {
        if (page < 0 || size < 1 || size > 50) throw new SubmissionConflictException("Invalid history bounds");
    }
}
