package com.ieltsaitutor.practice.generator.dto;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.rag.domain.Skill;

public record JobStatusResponse(
        UUID jobId,
        UUID setId,
        Skill skill,
        String status,
        GenerationState practiceState,
        String errorMessage,
        Instant createdAt,
        Instant completedAt) {
}
