package com.ieltsaitutor.practice.generator.domain;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.Skill;

public record PracticeGenerationJob(
        UUID id,
        UUID sourceId,
        UUID blueprintId,
        Skill skill,
        String status,
        String modelId,
        String promptTemplateVersion,
        String errorMessage,
        UUID createdBy,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {
}
