package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.ieltsaitutor.results.LearnerResult;
import com.ieltsaitutor.results.ResultStatus;

public record MockTestResult(
        UUID sessionId,
        UUID userId,
        String mockTestId,
        String mockTestVersion,
        String sessionStatus,
        ResultStatus resultStatus,
        Double estimatedOverallBand,
        String estimatedBandLabel,
        String aiDisclaimer,
        Instant completedAt,
        int totalDurationSeconds,
        List<MockTestSectionResult> sectionResults) {

    public static final String DEFAULT_AI_DISCLAIMER = "Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.";

    public MockTestResult {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(mockTestId, "mockTestId must not be null");
        Objects.requireNonNull(resultStatus, "resultStatus must not be null");
        sectionResults = sectionResults == null ? List.of() : List.copyOf(sectionResults);
        aiDisclaimer = aiDisclaimer != null ? aiDisclaimer.trim() : DEFAULT_AI_DISCLAIMER;
        estimatedBandLabel = estimatedOverallBand != null ? "Band ước lượng tổng thể" : null;
    }

    public record MockTestSectionResult(
            int sectionOrder,
            String skill,
            String practiceId,
            UUID submissionId,
            String sectionStatus,
            LearnerResult learnerResult,
            String statusMessage) {

        public MockTestSectionResult {
            Objects.requireNonNull(skill, "skill must not be null");
            Objects.requireNonNull(sectionStatus, "sectionStatus must not be null");
        }
    }
}
