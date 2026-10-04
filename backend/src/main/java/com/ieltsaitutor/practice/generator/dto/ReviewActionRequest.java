package com.ieltsaitutor.practice.generator.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewActionRequest(
    @NotBlank String action,
    String feedbackNotes,
    String revisionInstructions,
    String targetItemNumber
) {}
