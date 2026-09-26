package com.ieltsaitutor.practice.generator.service;

import java.time.Instant;
import java.util.UUID;

public record PracticeProvenanceRecord(
        String setId,
        UUID generationJobId,
        UUID blueprintId,
        UUID approverId,
        Instant approvedAt,
        String provenanceDetails) {

    public PracticeProvenanceRecord {
        if (provenanceDetails == null) provenanceDetails = "{}";
    }
}
