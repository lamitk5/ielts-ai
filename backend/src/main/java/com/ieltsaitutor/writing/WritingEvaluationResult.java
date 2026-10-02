package com.ieltsaitutor.writing;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record WritingEvaluationResult(
        UUID id,
        UUID versionId,
        int evaluationVersion,
        Double overallBandEstimate,
        Map<String, String> criteria,
        List<String> strengths,
        List<String> issues,
        List<String> suggestions,
        List<String> evidenceSpans,
        List<String> priorityImprovements,
        String groundingStatus,
        String disclaimer,
        String status,
        Instant createdAt) {

    public static final String STANDARD_DISCLAIMER = "Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.";

    public WritingEvaluationResult {
        criteria = criteria != null ? Map.copyOf(criteria) : Map.of();
        strengths = strengths != null ? List.copyOf(strengths) : List.of();
        issues = issues != null ? List.copyOf(issues) : List.of();
        suggestions = suggestions != null ? List.copyOf(suggestions) : List.of();
        evidenceSpans = evidenceSpans != null ? List.copyOf(evidenceSpans) : List.of();
        priorityImprovements = priorityImprovements != null ? List.copyOf(priorityImprovements) : List.of();
        groundingStatus = groundingStatus != null ? groundingStatus : "NOT_ENABLED";
        disclaimer = disclaimer != null ? disclaimer : STANDARD_DISCLAIMER;
        status = Objects.requireNonNullElse(status, "UNAVAILABLE");
    }

    public static WritingEvaluationResult unavailable(UUID versionId) {
        return new WritingEvaluationResult(
                UUID.randomUUID(),
                versionId,
                1,
                null,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "NOT_ENABLED",
                "Chưa có đánh giá AI khả dụng cho bài viết này.",
                "UNAVAILABLE",
                Instant.now());
    }

    public static WritingEvaluationResult malformed(UUID versionId) {
        return new WritingEvaluationResult(
                UUID.randomUUID(),
                versionId,
                1,
                null,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "NOT_ENABLED",
                "Phản hồi từ AI không đúng cấu trúc yêu cầu.",
                "MALFORMED",
                Instant.now());
    }

    public static WritingEvaluationResult failed(UUID versionId, String message) {
        return new WritingEvaluationResult(
                UUID.randomUUID(),
                versionId,
                1,
                null,
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "NOT_ENABLED",
                message != null ? message : "Đánh giá AI tạm thời không khả dụng.",
                "FAILED",
                Instant.now());
    }
}
