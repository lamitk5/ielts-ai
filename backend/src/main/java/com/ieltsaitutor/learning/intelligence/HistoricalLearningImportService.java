package com.ieltsaitutor.learning.intelligence;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HistoricalLearningImportService {
    private final LearningEventIngestionService ingestion;
    public HistoricalLearningImportService(LearningEventIngestionService ingestion) { this.ingestion = ingestion; }

    public void importSupported(UUID userId, List<HistoricalEvidence> evidence) {
        if (evidence == null) return;
        evidence.forEach(item -> ingestion.record(userId, new LearningEventRequest(LearningEventType.PRACTICE_COMPLETED,
                item.skill(), null, item.practiceSetId(), null, null, null, "historical:" + item.sourceId(),
                Map.of("sourceType", "HISTORICAL_IMPORT", "score", item.score(), "total", item.total()),
                "historical:" + item.sourceId(), item.occurredAt())));
    }
}
