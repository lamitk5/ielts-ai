package com.ieltsaitutor.practice.generator.domain;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.rag.domain.RightsStatus;

public record PracticeGenerationSource(
        UUID id,
        String title,
        String sourceType,
        String author,
        RightsStatus rightsStatus,
        String licenseNote,
        String normalizedContent,
        String checksum,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public PracticeGenerationSource {
        if (licenseNote == null) licenseNote = "";
    }
}
