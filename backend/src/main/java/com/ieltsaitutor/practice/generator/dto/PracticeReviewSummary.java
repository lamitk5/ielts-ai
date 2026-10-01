package com.ieltsaitutor.practice.generator.dto;

import java.time.Instant;
import java.util.UUID;
import com.ieltsaitutor.practice.generator.domain.GenerationState;

public record PracticeReviewSummary(
    UUID setId,
    String title,
    GenerationState state,
    int versionCount,
    int totalQuestions,
    Instant lastActionAt,
    String lastActionBy
) {}
