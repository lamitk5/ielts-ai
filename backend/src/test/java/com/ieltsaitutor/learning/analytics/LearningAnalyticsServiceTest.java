package com.ieltsaitutor.learning.analytics;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.*;

class LearningAnalyticsServiceTest {
    @Test
    void aggregatesQuestionTypeAccuracyFromPersistedAnswerEvents() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        List<LearningEvent> events = List.of(
                event(userId, LearningEventType.QUESTION_CORRECT, "Matching Headings", now),
                event(userId, LearningEventType.QUESTION_INCORRECT, "Matching Headings", now.plusSeconds(1)),
                event(userId, LearningEventType.QUESTION_CORRECT, "Multiple Choice", now.plusSeconds(2)));

        LearningAnalytics analytics = new LearningAnalyticsService().build(userId, events, List.of(), List.of());

        assertEquals(50.0, analytics.questionTypes().stream()
                .filter(item -> item.questionType().equals("Matching Headings"))
                .findFirst().orElseThrow().accuracyPercent());
        assertEquals("Matching Headings", analytics.weakestArea());
        assertTrue(analytics.hasEvidence());
    }

    private static LearningEvent event(UUID userId, LearningEventType type, String questionType, Instant at) {
        return new LearningEvent(UUID.randomUUID(), userId, type, Skill.READING, null, "set-1", null,
                UUID.randomUUID().toString(), null, null, Map.of("questionType", questionType), null, at, at);
    }
}
