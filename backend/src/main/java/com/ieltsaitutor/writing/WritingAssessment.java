package com.ieltsaitutor.writing;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record WritingAssessment(String status, UUID userId, String taskId, Double overallBandEstimate,
        Map<String, String> criteria, List<String> strengths, List<String> issues, List<String> suggestions,
        List<String> citations, String groundingStatus, boolean grounded, String disclaimer, Instant createdAt,
        @JsonIgnore String submittedText, int wordCount) {
    public static WritingAssessment unavailable(UUID userId, String taskId) {
        return new WritingAssessment("UNAVAILABLE", userId, taskId, null, Map.of(), List.of(), List.of(), List.of(),
                List.of(), "NOT_ENABLED", false, "Chưa có đánh giá AI khả dụng cho bài viết này.", Instant.now(), null, 0);
    }
}
