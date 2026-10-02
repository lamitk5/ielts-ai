package com.ieltsaitutor.writing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class WritingVersionComparisonService {

    public WritingVersionComparisonView compare(
            WritingSubmissionVersion baseVersion,
            WritingEvaluationResult baseEval,
            WritingSubmissionVersion targetVersion,
            WritingEvaluationResult targetEval) {

        Objects.requireNonNull(baseVersion, "baseVersion must not be null");
        Objects.requireNonNull(targetVersion, "targetVersion must not be null");

        List<String> baseParagraphs = splitParagraphs(baseVersion.responseText());
        List<String> targetParagraphs = splitParagraphs(targetVersion.responseText());

        boolean evalsAvailable = baseEval != null && targetEval != null
                && "GRADED".equals(baseEval.status()) && "GRADED".equals(targetEval.status())
                && baseEval.overallBandEstimate() != null && targetEval.overallBandEstimate() != null;

        Double baseBand = evalsAvailable ? baseEval.overallBandEstimate() : null;
        Double targetBand = evalsAvailable ? targetEval.overallBandEstimate() : null;
        Double bandDelta = evalsAvailable ? round(targetBand - baseBand) : null;

        Map<String, String> baseCriteria = evalsAvailable ? baseEval.criteria() : Map.of();
        Map<String, String> targetCriteria = evalsAvailable ? targetEval.criteria() : Map.of();

        List<String> resolvedIssues = new ArrayList<>();
        List<String> repeatedIssues = new ArrayList<>();
        List<String> newIssues = new ArrayList<>();

        if (evalsAvailable) {
            List<String> baseIssues = baseEval.issues();
            List<String> targetIssues = targetEval.issues();

            for (String issue : baseIssues) {
                if (containsSimilar(targetIssues, issue)) {
                    repeatedIssues.add(issue);
                } else {
                    resolvedIssues.add(issue);
                }
            }

            for (String issue : targetIssues) {
                if (!containsSimilar(baseIssues, issue)) {
                    newIssues.add(issue);
                }
            }
        }

        return new WritingVersionComparisonView(
                targetVersion.submissionId(),
                baseVersion.versionNumber(),
                targetVersion.versionNumber(),
                baseParagraphs,
                targetParagraphs,
                evalsAvailable,
                baseBand,
                targetBand,
                bandDelta,
                baseCriteria,
                targetCriteria,
                resolvedIssues,
                repeatedIssues,
                newIssues);
    }

    private List<String> splitParagraphs(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("\\R\\s*\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private boolean containsSimilar(List<String> list, String item) {
        if (list == null || item == null) return false;
        String normalizedItem = item.trim().toLowerCase();
        return list.stream().anyMatch(s -> s.trim().toLowerCase().equals(normalizedItem));
    }

    private Double round(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
