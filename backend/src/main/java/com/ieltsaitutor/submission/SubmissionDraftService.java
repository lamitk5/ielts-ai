package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionDraftService {
    private final PracticeSubmissionRepository submissions;
    private final SubmissionDraftRepository drafts;

    public SubmissionDraftService(PracticeSubmissionRepository submissions, SubmissionDraftRepository drafts) {
        this.submissions = submissions;
        this.drafts = drafts;
    }

    @Transactional
    public SubmissionDraftSnapshot autosave(SubmissionDraftCommand command) {
        PracticeSubmission current = submissions.findByOwnerAndId(command.userId(), command.submissionId())
                .orElseThrow(() -> new SubmissionConflictException("Submission is not available"));
        if (!current.editable()) throw new SubmissionConflictException("Submitted submissions are immutable");

        var latest = drafts.findBySubmissionId(command.submissionId());
        if (latest.isPresent() && Objects.equals(latest.get().idempotencyKey(), normalize(command.idempotencyKey()))) {
            if (Objects.equals(latest.get().payload(), command.payload())) return latest.get();
            throw new SubmissionConflictException("Autosave idempotency key was reused with different content");
        }
        if (current.autosaveRevision() != command.expectedRevision()) {
            throw new DraftRevisionConflictException(latest.orElse(null));
        }

        Instant now = Instant.now();
        PracticeSubmission updated = submissions.updateAutosaveIfRevision(current.id(), command.expectedRevision(), now)
                .orElseThrow(() -> new DraftRevisionConflictException(drafts.findBySubmissionId(command.submissionId()).orElse(null)));
        return drafts.saveIfRevision(updated.id(), updated.userId(), command.payload(), command.expectedRevision(),
                        command.idempotencyKey(), now)
                .orElseThrow(() -> new DraftRevisionConflictException(drafts.findBySubmissionId(command.submissionId()).orElse(null)));
    }

    public SubmissionDraftSnapshot load(UUID userId, UUID submissionId) {
        submissions.findByOwnerAndId(userId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission is not available"));
        return drafts.findBySubmissionId(submissionId).orElse(null);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
