package com.ieltsaitutor.practice.generator.domain;

import java.time.Instant;
import java.util.UUID;

public record GeneratedPracticeVersion(
        UUID id,
        UUID setId,
        int versionNumber,
        String passageContent,
        String questionsPayload,
        String noveltyReport,
        Instant createdAt) {

    public GeneratedPracticeVersion {
        if (noveltyReport == null) noveltyReport = "{}";
    }
}
