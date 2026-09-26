package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.UUID;

public record MistakeRecord(UUID id, UUID userId, Skill skill, String practiceSetId, UUID attemptId,
        String questionId, String questionType, String learnerAnswerSnapshot, String correctAnswerRef,
        String category, MistakeMethod method, double confidence, String evidenceCode, String evidenceText,
        Instant detectedAt, Instant resolvedAt, MistakeStatus status, int classificationRevision) {}
