package com.ieltsaitutor.learning.intelligence;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LearningEventIngestionService {
    private static final Set<String> FORBIDDEN = Set.of("correctanswer", "answerkey", "band", "apikey",
            "token", "secret", "essay", "audio", "providerpayload", "reasoning");
    private final LearningEventRepository repository;
    private final Clock clock;
    private final Duration allowedSkew;

    @Autowired
    public LearningEventIngestionService(LearningEventRepository repository) {
        this(repository, Clock.systemUTC(), Duration.ofHours(24));
    }

    public LearningEventIngestionService(LearningEventRepository repository, Clock clock, Duration allowedSkew) {
        this.repository = repository;
        this.clock = clock;
        this.allowedSkew = allowedSkew;
    }

    public LearningEvent record(UUID userId, LearningEventRequest request) {
        if (userId == null || request == null || request.eventType() == null || request.skill() == null)
            throw new IllegalArgumentException("event identity is required");
        if (request.sourceReference() == null || request.sourceReference().isBlank())
            throw new IllegalArgumentException("source reference is required");
        if (request.clientEventId() != null && !request.clientEventId().isBlank()) {
            var existing = repository.findByClientEvent(userId, request.clientEventId());
            if (existing.isPresent()) return existing.get();
        }
        var sourceExisting = repository.findBySourceReference(userId, request.sourceReference());
        if (sourceExisting.isPresent()) return sourceExisting.get();
        Instant now = clock.instant();
        Instant occurredAt = request.occurredAt() == null ? now : request.occurredAt();
        if (occurredAt.isBefore(now.minus(allowedSkew)) || occurredAt.isAfter(now.plus(allowedSkew)))
            throw new IllegalArgumentException("event timestamp is outside allowed skew");
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();
        if (payload.size() > 24 || payload.keySet().stream().map(key -> key.toLowerCase(Locale.ROOT)).anyMatch(FORBIDDEN::contains))
            throw new IllegalArgumentException("event payload contains prohibited data");
        LearningEvent event = new LearningEvent(UUID.randomUUID(), userId, request.eventType(), request.skill(),
                request.sessionId(), request.practiceSetId(), request.attemptId(), request.questionId(),
                request.roadmapItemId(), request.sourceReference(), payload,
                request.clientEventId() == null || request.clientEventId().isBlank() ? null : request.clientEventId(),
                occurredAt, now);
        repository.save(event);
        return event;
    }
}
