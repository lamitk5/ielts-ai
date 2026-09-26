package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrendAnalyzerTest {
    @Test
    void insufficientSampleDoesNotInventTrend() {
        TrendAnalyzer analyzer = new TrendAnalyzer();
        assertEquals(EvidenceState.INSUFFICIENT_DATA,
                analyzer.analyze(UUID.randomUUID(), Skill.SPEAKING, List.of()).state());
    }

    @Test
    void comparableAttemptsCanImprove() {
        UUID userId = UUID.randomUUID();
        List<LearningEvent> events = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> event(userId,
                        index < 3 ? LearningEventType.QUESTION_INCORRECT : LearningEventType.QUESTION_CORRECT,
                        Instant.now().minusSeconds(100 - index), MapScore.of(index < 3 ? 2 : 8, 10)))
                .toList();
        assertEquals(EvidenceState.IMPROVING, new TrendAnalyzer().analyze(userId, Skill.READING, events).state());
    }

    private LearningEvent event(UUID userId, LearningEventType type, Instant at, MapScore score) {
        return new LearningEvent(UUID.randomUUID(), userId, type, Skill.READING, null, "set", null, "q", null,
                "source-" + at, java.util.Map.of("score", score.value()), null, at, at);
    }

    record MapScore(int value) { static MapScore of(int score, int total) { return new MapScore(score); } }
}
