package com.ieltsaitutor.practice.generator.domain;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.Skill;

public record GeneratedPracticeSet(
        UUID id,
        UUID jobId,
        Skill skill,
        String title,
        UUID currentVersionId,
        GenerationState state,
        String publishedSetId,
        UUID approvedBy,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt) {
}
