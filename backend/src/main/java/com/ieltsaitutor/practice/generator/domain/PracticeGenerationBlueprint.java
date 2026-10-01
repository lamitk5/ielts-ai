package com.ieltsaitutor.practice.generator.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.Skill;

public record PracticeGenerationBlueprint(
        UUID id,
        Skill skill,
        String title,
        BigDecimal targetBand,
        String blueprintSchema,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
