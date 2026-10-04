package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HistoricalLearningImportServiceTest {
    @Test
    void supportedHistoricalEvidenceIsTraceableAndNotInvented() {
        List<LearningEvent> events = new ArrayList<>();
        LearningEventRepository repository = new LearningEventRepository() {
            @Override public java.util.Optional<LearningEvent> findByClientEvent(UUID userId, String id) { return java.util.Optional.empty(); }
            @Override public java.util.Optional<LearningEvent> findBySourceReference(UUID userId, String source) { return java.util.Optional.empty(); }
            @Override public void save(LearningEvent event) { events.add(event); }
        };
        var ingestion = new LearningEventIngestionService(repository,
                Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC), Duration.ofHours(24));
        new HistoricalLearningImportService(ingestion).importSupported(UUID.randomUUID(), List.of(
                new HistoricalEvidence("import-1", Skill.READING, "set", 5, 10, Instant.parse("2026-09-26T00:00:00Z"))));

        assertEquals(1, events.size());
        assertEquals("HISTORICAL_IMPORT", events.get(0).payload().get("sourceType"));
    }
}
