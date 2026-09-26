package com.ieltsaitutor.practice.generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.ieltsaitutor.rag.domain.RightsStatus;

public record RegisterSourceRequest(
    @NotBlank String title,
    @NotBlank String rawText,
    @NotNull RightsStatus rightsStatus,
    String language,
    String skill
) {}
