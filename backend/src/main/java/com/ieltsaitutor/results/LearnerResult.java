package com.ieltsaitutor.results;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ieltsaitutor.assessment.objective.QuestionResultView;

public record LearnerResult(
        UUID submissionId,
        String skill,
        String practiceId,
        String practiceVersionId,
        String status,
        ResultStatus resultStatus,
        Instant submittedAt,
        long durationSeconds,
        Integer score,
        Integer total,
        BigDecimal accuracy,
        Double estimatedBand,
        String estimatedBandLabel,
        String aiDisclaimer,
        EvaluationSummary aiEvaluation,
        HumanReviewSummary humanReview,
        List<QuestionResultView> questionResults,
        List<VersionSummary> versions,
        SpeakingSummary speaking,
        List<ResultAction> actions,
        String availabilityMessage) {

    public LearnerResult {
        questionResults = questionResults == null ? List.of() : List.copyOf(questionResults);
        versions = versions == null ? List.of() : List.copyOf(versions);
        actions = actions == null ? List.of() : List.copyOf(actions);
        estimatedBandLabel = estimatedBand == null ? null : "Band ước lượng";
        aiDisclaimer = aiDisclaimer == null ? null : aiDisclaimer.trim();
    }

    public record EvaluationSummary(
            UUID evaluationId,
            int evaluationVersion,
            Double estimatedBand,
            Map<String, String> criteria,
            List<String> strengths,
            List<String> issues,
            List<String> suggestions,
            List<String> priorityImprovements,
            String groundingStatus,
            String status,
            String disclaimer,
            Instant createdAt) {
        public EvaluationSummary {
            criteria = criteria == null ? Map.of() : Map.copyOf(criteria);
            strengths = strengths == null ? List.of() : List.copyOf(strengths);
            issues = issues == null ? List.of() : List.copyOf(issues);
            suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
            priorityImprovements = priorityImprovements == null ? List.of() : List.copyOf(priorityImprovements);
        }
    }

    public record HumanReviewSummary(
            UUID reviewId,
            int reviewVersion,
            Double overallBand,
            Double fluencyCoherence,
            Double lexicalResource,
            Double grammaticalRange,
            Double pronunciation,
            String feedback,
            String status,
            UUID reviewerId,
            Instant createdAt) {}

    public record VersionSummary(UUID id, int versionNumber, int wordCount, Instant createdAt, String evaluationStatus) {}

    public record SpeakingSummary(
            UUID id,
            String promptId,
            String status,
            String transcript,
            String transcriptSource,
            boolean audioAvailable) {}
}
