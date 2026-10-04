package com.ieltsaitutor.mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.results.LearnerResult;
import com.ieltsaitutor.results.LearnerResultService;
import com.ieltsaitutor.results.ResultStatus;

@Service
public class MockTestGradingService {

    private final MockTestService mockTestService;
    private final LearnerResultService learnerResultService;

    @Autowired
    public MockTestGradingService(
            MockTestService mockTestService,
            @Autowired(required = false) LearnerResultService learnerResultService) {
        this.mockTestService = Objects.requireNonNull(mockTestService, "mockTestService must not be null");
        this.learnerResultService = learnerResultService;
    }

    public MockTestResult getResult(UUID userId, UUID sessionId) {
        MockTestSession session = mockTestService.getSession(userId, sessionId);

        List<MockTestResult.MockTestSectionResult> sectionResults = new ArrayList<>();
        List<Double> validBands = new ArrayList<>();
        boolean hasProcessing = false;
        boolean hasReady = false;
        boolean hasFailed = false;

        for (MockTestSection section : session.sections()) {
            LearnerResult learnerResult = null;
            String statusMsg = null;

            if (section.submissionId() != null && learnerResultService != null) {
                try {
                    learnerResult = learnerResultService.get(userId, section.submissionId());
                    if (learnerResult.resultStatus() == ResultStatus.READY) {
                        hasReady = true;
                        Double band = extractBand(learnerResult);
                        if (band != null) {
                            validBands.add(band);
                        }
                    } else if (learnerResult.resultStatus() == ResultStatus.PROCESSING
                            || learnerResult.resultStatus() == ResultStatus.PARTIALLY_AVAILABLE) {
                        hasProcessing = true;
                    } else if (learnerResult.resultStatus() == ResultStatus.FAILED) {
                        hasFailed = true;
                    }
                } catch (Exception ex) {
                    statusMsg = "Không thể tải kết quả phần thi: " + ex.getMessage();
                    hasFailed = true;
                }
            } else if (section.status() == MockTestSectionStatus.NOT_STARTED) {
                statusMsg = "Phần thi chưa hoàn thành";
            }

            sectionResults.add(new MockTestResult.MockTestSectionResult(
                    section.sectionOrder(),
                    section.skill(),
                    section.practiceId(),
                    section.submissionId(),
                    section.status().name(),
                    learnerResult,
                    statusMsg
            ));
        }

        ResultStatus overallStatus;
        if (hasProcessing) {
            overallStatus = ResultStatus.PARTIALLY_AVAILABLE;
        } else if (hasReady && !hasFailed) {
            overallStatus = ResultStatus.READY;
        } else if (hasReady && hasFailed) {
            overallStatus = ResultStatus.PARTIALLY_AVAILABLE;
        } else if (hasFailed) {
            overallStatus = ResultStatus.FAILED;
        } else {
            overallStatus = ResultStatus.PARTIALLY_AVAILABLE;
        }

        Double overallBand = null;
        // Calculate estimated overall band ONLY if all sections in session have valid bands
        if (!session.sections().isEmpty() && validBands.size() == session.sections().size()) {
            double sum = 0.0;
            for (Double b : validBands) {
                sum += b;
            }
            double rawAvg = sum / validBands.size();
            overallBand = roundToIeltsBand(rawAvg);
        }

        Instant completedAt = session.completedAt() != null ? session.completedAt() : session.updatedAt();

        return new MockTestResult(
                session.id(),
                session.userId(),
                session.mockTestId(),
                session.mockTestVersion(),
                session.status().name(),
                overallStatus,
                overallBand,
                overallBand != null ? "Band ước lượng tổng thể" : null,
                MockTestResult.DEFAULT_AI_DISCLAIMER,
                completedAt,
                session.elapsedSeconds(),
                sectionResults
        );
    }

    private Double extractBand(LearnerResult lr) {
        if (lr == null) return null;
        if (lr.estimatedBand() != null) return lr.estimatedBand();
        if (lr.aiEvaluation() != null && lr.aiEvaluation().estimatedBand() != null) return lr.aiEvaluation().estimatedBand();
        if (lr.humanReview() != null && lr.humanReview().overallBand() != null) return lr.humanReview().overallBand();
        return null;
    }

    private double roundToIeltsBand(double raw) {
        // IELTS rounding: e.g. 6.25 -> 6.5, 6.75 -> 7.0, 6.125 -> 6.0
        double multiplied = raw * 2.0;
        double rounded = Math.round(multiplied) / 2.0;
        return BigDecimal.valueOf(rounded).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
