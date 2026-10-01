package com.ieltsaitutor.practice.generator.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ExtractBlueprintRequest(
    UUID sourceId,
    String rawText,
    String passageTitle,
    String targetBand,
    String genre
) {}
