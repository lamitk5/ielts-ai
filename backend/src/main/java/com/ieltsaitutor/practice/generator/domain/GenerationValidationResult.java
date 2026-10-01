package com.ieltsaitutor.practice.generator.domain;

import java.time.Instant;
import java.util.UUID;

public record GenerationValidationResult(
        UUID id,
        UUID versionId,
        String validatorName,
        ValidationStatus status,
        String findings,
        Instant executedAt) {

    public GenerationValidationResult {
        if (findings == null) findings = "[]";
    }
}
