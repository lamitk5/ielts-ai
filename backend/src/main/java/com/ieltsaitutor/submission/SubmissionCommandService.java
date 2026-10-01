package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionCommandService {
    private final PracticeSubmissionRepository repository;
    private final SubmissionStateMachine stateMachine;

    public SubmissionCommandService(PracticeSubmissionRepository repository) {
        this(repository, new SubmissionStateMachine());
    }

    public SubmissionCommandService(PracticeSubmissionRepository repository, SubmissionStateMachine stateMachine) {
        this.repository = repository;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public PracticeSubmission transition(UUID ownerId, UUID submissionId, SubmissionCommand command) {
        PracticeSubmission current = repository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission is not available"));
        SubmissionStatus next = stateMachine.next(current.status(), command);
        return repository.updateStatus(submissionId, next, Instant.now());
    }

    @Transactional
    public PracticeSubmission retry(UUID ownerId, UUID submissionId, SubmissionStatus target) {
        PracticeSubmission current = repository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission is not available"));
        SubmissionStatus next = stateMachine.retry(current.status(), target);
        return repository.updateStatus(submissionId, next, Instant.now());
    }
}
