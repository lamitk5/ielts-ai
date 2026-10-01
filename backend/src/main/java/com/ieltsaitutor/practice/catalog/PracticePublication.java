package com.ieltsaitutor.practice.catalog;

import java.time.Instant;
import java.util.UUID;

public record PracticePublication(
        String publishedSetId,
        UUID generatedSetId,
        UUID generatedVersionId,
        String skill,
        boolean active,
        int publicationRevision,
        String provenanceReference,
        Instant publishedAt) {

    public PracticePublication {
        if (publishedSetId == null || publishedSetId.isBlank()) throw new IllegalArgumentException("publishedSetId is required");
        if (skill == null || skill.isBlank()) throw new IllegalArgumentException("skill is required");
        if (publicationRevision < 1) throw new IllegalArgumentException("publicationRevision must be positive");
        provenanceReference = provenanceReference == null ? "" : provenanceReference;
        publishedAt = publishedAt == null ? Instant.now() : publishedAt;
    }
}
