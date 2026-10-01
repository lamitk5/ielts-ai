package com.ieltsaitutor.practice.generator.domain;

import java.time.Instant;
import java.util.UUID;

public record PracticeReviewAction(
        UUID id,
        UUID setId,
        UUID versionId,
        UUID adminId,
        String action,
        String reviewerNotes,
        Instant createdAt) {

    public PracticeReviewAction {
        if (reviewerNotes == null) reviewerNotes = "";
    }
}
