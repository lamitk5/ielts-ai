package com.ieltsaitutor.submission;

import java.util.List;

public record SubmissionHistoryPage(List<PracticeSubmission> items, int page, int size, long total) {
    public SubmissionHistoryPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
