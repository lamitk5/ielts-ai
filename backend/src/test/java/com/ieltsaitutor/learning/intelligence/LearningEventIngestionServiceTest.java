package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LearningEventIngestionServiceTest {
    @Test
    void replayedClientEventIsAcceptedOnce() {
        InMemoryEvents repository = new InMemoryEvents();
        LearningEventIngestionService service = service(repository);
        UUID userId = UUID.randomUUID();
        LearningEventRequest request = request("client-1", "practice:attempt-1:q1", Instant.parse("2026-09-27T00:00:00Z"),
                Map.of("selectedAnswer", "B"));

        LearningEvent first = service.record(userId, request);
        LearningEvent replay = service.record(userId, request);

        assertEquals(first.id(), replay.id());
        assertEquals(1, repository.saved.size());
    }

    @Test
    void rejectsRawAnswersAndSecrets() {
        LearningEventIngestionService service = service(new InMemoryEvents());
        Map<String, Object> payload = new HashMap<>();
        payload.put("correctAnswer", "A");
        payload.put("apiKey", "secret");

        assertThrows(IllegalArgumentException.class,
                () -> service.record(UUID.randomUUID(), request("client-2", "source-2", Instant.now(), payload)));
    }

    @Test
    void rejectsEventOutsideBoundedClockSkew() {
        LearningEventIngestionService service = service(new InMemoryEvents());
        Instant now = Instant.parse("2026-09-27T00:00:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> service.record(UUID.randomUUID(), request("client-3", "source-3", now.minus(Duration.ofDays(2)), Map.of())));
    }

    private static LearningEventIngestionService service(InMemoryEvents repository) {
        return new LearningEventIngestionService(repository,
                Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC), Duration.ofHours(24));
    }

    private static LearningEventRequest request(String clientId, String source, Instant occurredAt, Map<String, Object> payload) {
        return new LearningEventRequest(LearningEventType.ANSWER_SUBMITTED, Skill.READING, null, "set-1", null,
                "q1", null, source, payload, clientId, occurredAt);
    }

    static final class InMemoryEvents implements LearningEventRepository {
        final Map<String, LearningEvent> events = new HashMap<>();
        final List<LearningEvent> saved = new ArrayList<>();
        @Override public Optional<LearningEvent> findByClientEvent(UUID userId, String clientEventId) {
            return Optional.ofNullable(events.get(userId + ":client:" + clientEventId));
        }
        @Override public Optional<LearningEvent> findBySourceReference(UUID userId, String sourceReference) {
            return Optional.ofNullable(events.get(userId + ":source:" + sourceReference));
        }
        @Override public void save(LearningEvent event) {
            saved.add(event);
            events.put(event.userId() + ":client:" + event.clientEventId(), event);
            events.put(event.userId() + ":source:" + event.sourceReference(), event);
        }
    }
}
