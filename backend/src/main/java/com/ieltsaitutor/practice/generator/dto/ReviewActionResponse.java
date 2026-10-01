package com.ieltsaitutor.practice.generator.dto;

import java.time.Instant;
import java.util.UUID;
import com.ieltsaitutor.practice.generator.domain.GenerationState;

public record ReviewActionResponse(
    UUID actionId,
    UUID setId,
    String action,
    GenerationState resultingState,
    UUID adminId,
    Instant performedAt,
    String message
) {}
