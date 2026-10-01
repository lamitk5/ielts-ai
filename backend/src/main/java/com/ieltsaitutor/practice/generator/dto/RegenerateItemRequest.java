package com.ieltsaitutor.practice.generator.dto;

import jakarta.validation.constraints.NotBlank;

public record RegenerateItemRequest(
    @NotBlank String questionId,
    String revisionInstructions,
    String preferredTaskType
) {}
