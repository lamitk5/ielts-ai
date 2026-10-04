package com.ieltsaitutor.writing;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record WritingVersionComparisonView(
        UUID submissionId,
        int baseVersionNumber,
        int targetVersionNumber,
        List<String> baseParagraphs,
        List<String> targetParagraphs,
        boolean evaluationsAvailable,
        Double baseBand,
        Double targetBand,
        Double bandDelta,
        Map<String, String> baseCriteria,
        Map<String, String> targetCriteria,
        List<String> resolvedIssues,
        List<String> repeatedIssues,
        List<String> newIssues) {

    public WritingVersionComparisonView {
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        baseParagraphs = baseParagraphs != null ? List.copyOf(baseParagraphs) : List.of();
        targetParagraphs = targetParagraphs != null ? List.copyOf(targetParagraphs) : List.of();
        baseCriteria = baseCriteria != null ? Map.copyOf(baseCriteria) : Map.of();
        targetCriteria = targetCriteria != null ? Map.copyOf(targetCriteria) : Map.of();
        resolvedIssues = resolvedIssues != null ? List.copyOf(resolvedIssues) : List.of();
        repeatedIssues = repeatedIssues != null ? List.copyOf(repeatedIssues) : List.of();
        newIssues = newIssues != null ? List.copyOf(newIssues) : List.of();
    }
}
