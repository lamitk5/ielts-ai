package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SubmissionFinalizeIdempotencyTest {
    @Test
    void finalizationStoresOneImmutableSnapshotWithServerTimingAndRetryIsSafe() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        PracticeSubmission inProgress = submission(owner, submissionId, SubmissionStatus.IN_PROGRESS, null, null, null);
        PracticeSubmission finalized = inProgress.withFinalized(Instant.parse("2026-10-01T10:01:30Z"),
                "submit-1", SubmissionFinalizationService.contentHash(Map.of("q1", "B")));
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionAnswerRepository answers = mock(SubmissionAnswerRepository.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(inProgress), Optional.of(finalized));
        when(submissions.finalizeIfEditable(eq(submissionId), eq("submit-1"), any(), any(Instant.class)))
                .thenReturn(Optional.of(finalized));

        SubmissionFinalizationService service = new SubmissionFinalizationService(submissions, answers,
                () -> Instant.parse("2026-10-01T10:01:30Z"));
        PracticeSubmission first = service.finalizeSubmission(owner, submissionId, Map.of("q1", "B"), "submit-1");
        PracticeSubmission retry = service.finalizeSubmission(owner, submissionId, Map.of("q1", "B"), "submit-1");

        assertEquals(first, finalized);
        assertEquals(first, retry);
        verify(answers).save(any(SubmissionAnswerSnapshot.class));
    }

    @Test
    void changedPayloadAfterFinalizationIsRejectedAndNeverOverwritten() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        String originalHash = SubmissionFinalizationService.contentHash(Map.of("q1", "B"));
        PracticeSubmission finalized = submission(owner, submissionId, SubmissionStatus.SUBMITTED,
                "submit-1", originalHash, Instant.parse("2026-10-01T10:01:30Z"));
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionAnswerRepository answers = mock(SubmissionAnswerRepository.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(finalized));

        SubmissionFinalizationService service = new SubmissionFinalizationService(submissions, answers, Instant::now);

        assertThrows(SubmissionConflictException.class,
                () -> service.finalizeSubmission(owner, submissionId, Map.of("q1", "A"), "submit-2"));
        verify(submissions, never()).finalizeIfEditable(any(), any(), any(), any());
        verify(answers, never()).save(any());
    }

    private static PracticeSubmission submission(UUID owner, UUID id, SubmissionStatus status,
            String submitKey, String hash, Instant submittedAt) {
        Instant started = Instant.parse("2026-10-01T10:00:00Z");
        return new PracticeSubmission(id, owner, "READING", "set", "version-1", "published", 1, status,
                started, started, submittedAt, null, 0, "start", submitKey, hash, false, started,
                submittedAt == null ? started : submittedAt);
    }
}
