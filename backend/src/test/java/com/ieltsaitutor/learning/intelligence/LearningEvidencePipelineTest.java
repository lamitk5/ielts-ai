package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LearningEvidencePipelineTest {
    @Test
    void practiceCompletionIsAuthenticatedTraceableAndReplaySafe() {
        List<LearningEvent> events = new ArrayList<>();
        LearningEventIngestionService ingestion = new LearningEventIngestionService(new LearningEventRepository() {
            @Override public java.util.Optional<LearningEvent> findByClientEvent(UUID userId, String id) { return events.stream().filter(e -> id.equals(e.clientEventId())).findFirst(); }
            @Override public java.util.Optional<LearningEvent> findBySourceReference(UUID userId, String source) { return events.stream().filter(e -> source.equals(e.sourceReference())).findFirst(); }
            @Override public void save(LearningEvent event) { events.add(event); }
        }, Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC), Duration.ofHours(24));
        LearningEvidencePipeline pipeline = new LearningEvidencePipeline(ingestion);
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();

        pipeline.practiceCompleted(userId, Skill.READING, "set-1", attemptId, 7, 10, Instant.parse("2026-09-27T00:00:00Z"));
        pipeline.practiceCompleted(userId, Skill.READING, "set-1", attemptId, 7, 10, Instant.parse("2026-09-27T00:00:00Z"));

        assertEquals(1, events.size());
        assertEquals(LearningEventType.PRACTICE_COMPLETED, events.get(0).eventType());
        assertTrue(events.get(0).payload().containsKey("score"));
    }

    @Test
    void writingAndSpeakingEventsDoNotPersistRawContent() {
        List<LearningEvent> events = new ArrayList<>();
        LearningEventIngestionService ingestion = new LearningEventIngestionService(new LearningEventRepository() {
            @Override public java.util.Optional<LearningEvent> findByClientEvent(UUID userId, String id) { return java.util.Optional.empty(); }
            @Override public java.util.Optional<LearningEvent> findBySourceReference(UUID userId, String source) { return java.util.Optional.empty(); }
            @Override public void save(LearningEvent event) { events.add(event); }
        }, Clock.systemUTC(), Duration.ofHours(24));
        LearningEvidencePipeline pipeline = new LearningEvidencePipeline(ingestion);

        pipeline.writingSubmitted(UUID.randomUUID(), "task-1", 180, Instant.now());
        pipeline.speakingSubmitted(UUID.randomUUID(), "speaking-p1-01", true, Instant.now());

        assertTrue(events.stream().allMatch(event -> event.payload().values().stream().noneMatch(value -> String.valueOf(value).contains("essay"))));
        assertTrue(events.stream().allMatch(event -> !event.payload().containsKey("transcript")));
    }
}
