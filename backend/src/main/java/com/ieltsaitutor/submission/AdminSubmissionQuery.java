package com.ieltsaitutor.submission;

public record AdminSubmissionQuery(String skill, SubmissionStatus status, int page, int size) {
    public AdminSubmissionQuery {
        if (page < 0) throw new IllegalArgumentException("page must be non-negative");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
    }
}
