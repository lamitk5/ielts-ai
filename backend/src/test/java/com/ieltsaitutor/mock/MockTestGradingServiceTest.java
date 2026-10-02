package com.ieltsaitutor.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.results.LearnerResult;
import com.ieltsaitutor.results.LearnerResultService;
import com.ieltsaitutor.results.ResultStatus;

class MockTestGradingServiceTest {

    private MockTestService mockTestService;
    private LearnerResultService learnerResultService;
    private MockTestGradingService gradingService;

    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockTestService = mock(MockTestService.class);
        learnerResultService = mock(LearnerResultService.class);
        gradingService = new MockTestGradingService(mockTestService, learnerResultService);
    }

    private LearnerResult createObjectiveResult(UUID subId, String skill, int score, int total, Double band) {
        return new LearnerResult(
                subId, skill, "practice-" + skill, "v1", "GRADED", ResultStatus.READY,
                Instant.now(), 1800, score, total, BigDecimal.valueOf(0.85), band, "Band ước lượng",
                null, null, null, List.of(), List.of(), null, List.of(), null
        );
    }

    private LearnerResult createWritingResult(UUID subId, ResultStatus status, Double band) {
        LearnerResult.EvaluationSummary eval = status == ResultStatus.READY ? new LearnerResult.EvaluationSummary(
                UUID.randomUUID(), 1, band, Map.of("task_achievement", "7.0"),
                List.of("Clear thesis"), List.of(), List.of(), List.of(),
                "COMPLIANT", "GRADED", "Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.", Instant.now()
        ) : null;

        return new LearnerResult(
                subId, "WRITING", "practice-writing", "v1", status == ResultStatus.READY ? "GRADED" : "SUBMITTED",
                status, Instant.now(), 3600, null, null, null, band, "Band ước lượng",
                "Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.", eval, null, List.of(),
                List.of(), null, List.of(), status == ResultStatus.READY ? null : "Chưa có đánh giá AI cho phiên bản này."
        );
    }

    private LearnerResult createSpeakingResult(UUID subId, ResultStatus status, Double band) {
        LearnerResult.HumanReviewSummary review = status == ResultStatus.READY ? new LearnerResult.HumanReviewSummary(
                UUID.randomUUID(), 1, band, 7.0, 7.0, 7.0, 7.0, "Good fluency", "COMPLETED", UUID.randomUUID(), Instant.now()
        ) : null;

        return new LearnerResult(
                subId, "SPEAKING", "practice-speaking", "v1", status == ResultStatus.READY ? "GRADED" : "SUBMITTED",
                status, Instant.now(), 900, null, null, null, null, null,
                null, null, review, List.of(), List.of(),
                new LearnerResult.SpeakingSummary(UUID.randomUUID(), "prompt-1", "PROCESSED", "Transcript here", "whisper", true),
                List.of(), status == ResultStatus.READY ? null : "Bài nói đang chờ đánh giá thủ công."
        );
    }

    @Test
    void partialGradingWhenWritingAndSpeakingArePending() {
        UUID subListening = UUID.randomUUID();
        UUID subReading = UUID.randomUUID();
        UUID subWriting = UUID.randomUUID();
        UUID subSpeaking = UUID.randomUUID();

        Instant now = Instant.now();
        List<MockTestSection> sections = List.of(
                new MockTestSection(UUID.randomUUID(), sessionId, 0, "LISTENING", "p-lis", "v1", "set-1", subListening, 1800, MockTestSectionStatus.COMPLETED, now, now),
                new MockTestSection(UUID.randomUUID(), sessionId, 1, "READING", "p-read", "v1", "set-2", subReading, 3600, MockTestSectionStatus.COMPLETED, now, now),
                new MockTestSection(UUID.randomUUID(), sessionId, 2, "WRITING", "p-wri", "v1", "set-3", subWriting, 3600, MockTestSectionStatus.COMPLETED, now, now),
                new MockTestSection(UUID.randomUUID(), sessionId, 3, "SPEAKING", "p-spk", "v1", "set-4", subSpeaking, 900, MockTestSectionStatus.COMPLETED, now, now)
        );

        MockTestSession session = new MockTestSession(
                sessionId, userId, "mock-01", "v1", MockTestSessionStatus.COMPLETED,
                3, 9900, 9500, now.minusSeconds(9500), now, now, now, now, sections
        );

        when(mockTestService.getSession(userId, sessionId)).thenReturn(session);
        when(learnerResultService.get(userId, subListening)).thenReturn(createObjectiveResult(subListening, "LISTENING", 32, 40, 7.5));
        when(learnerResultService.get(userId, subReading)).thenReturn(createObjectiveResult(subReading, "READING", 35, 40, 8.0));
        when(learnerResultService.get(userId, subWriting)).thenReturn(createWritingResult(subWriting, ResultStatus.PROCESSING, null));
        when(learnerResultService.get(userId, subSpeaking)).thenReturn(createSpeakingResult(subSpeaking, ResultStatus.PARTIALLY_AVAILABLE, null));

        MockTestResult result = gradingService.getResult(userId, sessionId);

        assertNotNull(result);
        assertEquals(ResultStatus.PARTIALLY_AVAILABLE, result.resultStatus());
        assertNull(result.estimatedOverallBand(), "Overall band must be null when some skills are still pending");
        assertEquals(4, result.sectionResults().size());
        assertTrue(result.aiDisclaimer().contains("Không phải điểm thi IELTS chính thức"));
    }

    @Test
    void fullGradingWhenAllFourSkillsAreGraded() {
        UUID subListening = UUID.randomUUID();
        UUID subReading = UUID.randomUUID();
        UUID subWriting = UUID.randomUUID();
        UUID subSpeaking = UUID.randomUUID();

        Instant now = Instant.now();
        List<MockTestSection> sections = List.of(
                new MockTestSection(UUID.randomUUID(), sessionId, 0, "LISTENING", "p-lis", "v1", "set-1", subListening, 1800, MockTestSectionStatus.COMPLETED, now, now),
                new MockTestSection(UUID.randomUUID(), sessionId, 1, "READING", "p-read", "v1", "set-2", subReading, 3600, MockTestSectionStatus.COMPLETED, now, now),
                new MockTestSection(UUID.randomUUID(), sessionId, 2, "WRITING", "p-wri", "v1", "set-3", subWriting, 3600, MockTestSectionStatus.COMPLETED, now, now),
                new MockTestSection(UUID.randomUUID(), sessionId, 3, "SPEAKING", "p-spk", "v1", "set-4", subSpeaking, 900, MockTestSectionStatus.COMPLETED, now, now)
        );

        MockTestSession session = new MockTestSession(
                sessionId, userId, "mock-01", "v1", MockTestSessionStatus.COMPLETED,
                3, 9900, 9500, now.minusSeconds(9500), now, now, now, now, sections
        );

        when(mockTestService.getSession(userId, sessionId)).thenReturn(session);
        when(learnerResultService.get(userId, subListening)).thenReturn(createObjectiveResult(subListening, "LISTENING", 30, 40, 7.0));
        when(learnerResultService.get(userId, subReading)).thenReturn(createObjectiveResult(subReading, "READING", 34, 40, 7.5));
        when(learnerResultService.get(userId, subWriting)).thenReturn(createWritingResult(subWriting, ResultStatus.READY, 6.5));
        when(learnerResultService.get(userId, subSpeaking)).thenReturn(createSpeakingResult(subSpeaking, ResultStatus.READY, 7.0));

        MockTestResult result = gradingService.getResult(userId, sessionId);

        assertNotNull(result);
        assertEquals(ResultStatus.READY, result.resultStatus());
        // Average: (7.0 + 7.5 + 6.5 + 7.0) / 4 = 28.0 / 4 = 7.0
        assertEquals(7.0, result.estimatedOverallBand());
        assertEquals("Band ước lượng tổng thể", result.estimatedBandLabel());
        assertTrue(result.aiDisclaimer().contains("Không phải điểm thi IELTS chính thức"));
    }
}
