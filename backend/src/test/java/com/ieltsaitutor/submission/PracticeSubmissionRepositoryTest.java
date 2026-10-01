package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PracticeSubmissionRepositoryTest {
    @Test
    void canonicalSubmissionKeepsOwnerPinnedVersionAndInitialRevision() {
        UUID submissionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant startedAt = Instant.parse("2026-10-01T10:00:00Z");

        PracticeSubmission submission = new PracticeSubmission(
                submissionId,
                ownerId,
                "READING",
                "reading-set-1",
                "reading-set-1:v3",
                "published-reading-1",
                3,
                SubmissionStatus.DRAFT,
                startedAt,
                startedAt,
                null,
                null,
                0L,
                null,
                null,
                null,
                true,
                startedAt,
                startedAt);

        assertEquals(submissionId, submission.id());
        assertEquals(ownerId, submission.userId());
        assertEquals("READING", submission.skill());
        assertEquals("reading-set-1:v3", submission.practiceVersionId());
        assertEquals("published-reading-1", submission.publishedSetId());
        assertEquals(3, submission.publicationRevision());
        assertEquals(SubmissionStatus.DRAFT, submission.status());
        assertEquals(0L, submission.autosaveRevision());
        assertNull(submission.submittedAt());
        assertEquals(0, submission.durationSeconds());
    }
}
