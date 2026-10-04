package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LearningEvidencePipeline {
    private final LearningEventIngestionService ingestion;

    public LearningEvidencePipeline(LearningEventIngestionService ingestion) { this.ingestion = ingestion; }

    public LearningEvent practiceCompleted(UUID userId, Skill skill, String setId, UUID attemptId, int score, int total, Instant occurredAt) {
        return record(userId, new LearningEventRequest(LearningEventType.PRACTICE_COMPLETED, skill, null, setId, attemptId,
                null, null, "practice:" + attemptId + ":completed", Map.of("score", score, "total", total),
                "practice:" + attemptId + ":completed", occurredAt));
    }

    public LearningEvent writingSubmitted(UUID userId, String taskId, int wordCount, Instant occurredAt) {
        return record(userId, new LearningEventRequest(LearningEventType.WRITING_SUBMITTED, Skill.WRITING, null, null, null,
                taskId, null, "writing:" + userId + ":" + taskId + ":" + occurredAt.toEpochMilli(), Map.of("wordCount", wordCount),
                "writing:" + userId + ":" + taskId + ":" + occurredAt.toEpochMilli(), occurredAt));
    }

    public LearningEvent speakingSubmitted(UUID userId, String promptId, boolean hasTranscript, Instant occurredAt) {
        return record(userId, new LearningEventRequest(LearningEventType.SPEAKING_SUBMITTED, Skill.SPEAKING, null, null, null,
                promptId, null, "speaking:" + userId + ":" + promptId + ":" + occurredAt.toEpochMilli(), Map.of("hasTranscript", hasTranscript),
                "speaking:" + userId + ":" + promptId + ":" + occurredAt.toEpochMilli(), occurredAt));
    }

    private LearningEvent record(UUID userId, LearningEventRequest request) { return ingestion.record(userId, request); }
}
