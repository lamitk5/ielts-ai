package com.ieltsaitutor.results;

import com.ieltsaitutor.learning.intelligence.LearningEvent;
import com.ieltsaitutor.learning.intelligence.LearningEventIngestionService;
import com.ieltsaitutor.learning.intelligence.LearningEventRequest;
import com.ieltsaitutor.learning.intelligence.LearningEventType;
import com.ieltsaitutor.learning.intelligence.Skill;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Publishes only server-derived result facts using stable, idempotent references. */
@Service
public class TrustedResultPublisher {
    private final LearningEventIngestionService ingestion;

    public TrustedResultPublisher(LearningEventIngestionService ingestion) { this.ingestion = ingestion; }

    public LearningEvent publish(UUID ownerId, LearnerResult result) {
        if (ownerId == null || result == null || result.submissionId() == null)
            throw new IllegalArgumentException("result identity is required");
        Skill skill = Skill.valueOf(result.skill().toUpperCase());
        LearningEventType type = switch (skill) {
            case READING, LISTENING -> LearningEventType.PRACTICE_COMPLETED;
            case WRITING -> LearningEventType.WRITING_SUBMITTED;
            case SPEAKING -> LearningEventType.SPEAKING_SUBMITTED;
        };
        String source = "result:" + result.submissionId() + ":" + type.name().toLowerCase();
        Map<String, Object> payload = switch (type) {
            case PRACTICE_COMPLETED -> Map.of("score", result.score() == null ? 0 : result.score(),
                    "total", result.total() == null ? 0 : result.total());
            case WRITING_SUBMITTED -> Map.of("wordCount", result.versions().stream()
                    .mapToInt(version -> version.wordCount()).max().orElse(0));
            case SPEAKING_SUBMITTED -> Map.of("hasTranscript", result.speaking() != null
                    && result.speaking().transcript() != null && !result.speaking().transcript().isBlank());
            default -> throw new IllegalStateException("Unsupported trusted result event");
        };
        return ingestion.record(ownerId, new LearningEventRequest(type, skill, null, result.practiceId(),
                result.submissionId(), null, null, source, payload, source, result.submittedAt() == null ? Instant.now() : result.submittedAt()));
    }
}
