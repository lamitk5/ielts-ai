package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SubmissionAutosaveTest {
    @Test
    void autosaveIncrementsRevisionAndReturnsAuthoritativeSnapshot() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        PracticeSubmission current = submission(owner, submissionId, SubmissionStatus.IN_PROGRESS, 0);
        PracticeSubmission updated = current.withAutosave(1, Instant.now());
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionDraftRepository drafts = mock(SubmissionDraftRepository.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(current));
        when(submissions.updateAutosaveIfRevision(any(), any(Long.class), any(Instant.class)))
                .thenReturn(Optional.of(updated));
        SubmissionDraftSnapshot saved = new SubmissionDraftSnapshot(submissionId, owner,
                Map.of("q1", "B"), 1, "save-1", Instant.now());
        when(drafts.saveIfRevision(any(), any(), any(), any(Long.class), any(), any(Instant.class)))
                .thenReturn(Optional.of(saved));

        SubmissionDraftSnapshot result = new SubmissionDraftService(submissions, drafts).autosave(
                new SubmissionDraftCommand(submissionId, owner, Map.of("q1", "B"), 0, "save-1"));

        assertEquals(1, result.revision());
        assertEquals(Map.of("q1", "B"), result.payload());
    }

    @Test
    void staleRevisionReturnsLatestSnapshotWithoutOverwritingIt() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        PracticeSubmission current = submission(owner, submissionId, SubmissionStatus.IN_PROGRESS, 2);
        SubmissionDraftSnapshot latest = new SubmissionDraftSnapshot(submissionId, owner,
                Map.of("q1", "A"), 2, "save-2", Instant.now());
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionDraftRepository drafts = mock(SubmissionDraftRepository.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(current));
        when(drafts.findBySubmissionId(submissionId)).thenReturn(Optional.of(latest));

        DraftRevisionConflictException error = assertThrows(DraftRevisionConflictException.class,
                () -> new SubmissionDraftService(submissions, drafts).autosave(
                        new SubmissionDraftCommand(submissionId, owner, Map.of("q1", "B"), 1, "save-3")));

        assertEquals(latest, error.latestSnapshot());
        verify(submissions, never()).updateAutosaveIfRevision(any(), any(Long.class), any(Instant.class));
        verify(drafts, never()).saveIfRevision(any(), any(), any(), any(Long.class), any(), any(Instant.class));
    }

    @Test
    void duplicateAutosaveIsSafeAndSubmittedSubmissionsAreNotEditable() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        PracticeSubmission current = submission(owner, submissionId, SubmissionStatus.IN_PROGRESS, 1);
        SubmissionDraftSnapshot previous = new SubmissionDraftSnapshot(submissionId, owner,
                Map.of("q1", "B"), 1, "save-1", Instant.now());
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionDraftRepository drafts = mock(SubmissionDraftRepository.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(current));
        when(drafts.findBySubmissionId(submissionId)).thenReturn(Optional.of(previous));

        SubmissionDraftService service = new SubmissionDraftService(submissions, drafts);
        assertEquals(previous, service.autosave(new SubmissionDraftCommand(submissionId, owner,
                Map.of("q1", "B"), 0, "save-1")));

        when(submissions.findByOwnerAndId(owner, submissionId))
                .thenReturn(Optional.of(submission(owner, submissionId, SubmissionStatus.SUBMITTED, 1)));
        assertThrows(SubmissionConflictException.class, () -> service.autosave(new SubmissionDraftCommand(
                submissionId, owner, Map.of("q1", "C"), 1, "save-2")));
    }

    private static PracticeSubmission submission(UUID owner, UUID id, SubmissionStatus status, long revision) {
        Instant now = Instant.now();
        return new PracticeSubmission(id, owner, "READING", "set", "version-1", "published", 1,
                status, now, now, null, null, revision, "start", null, null, false, now, now);
    }
}
