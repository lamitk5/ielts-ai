package com.ieltsaitutor.submission;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CanonicalSubmissionService {
    private final PracticeSubmissionRepository repository;
    private final SubmissionPracticeResolver practiceResolver;
    private final SubmissionDraftService draftService;
    private final SubmissionFinalizationService finalizationService;

    public CanonicalSubmissionService(PracticeSubmissionRepository repository,
            SubmissionPracticeResolver practiceResolver, SubmissionDraftService draftService,
            SubmissionFinalizationService finalizationService) {
        this.repository = repository;
        this.practiceResolver = practiceResolver;
        this.draftService = draftService;
        this.finalizationService = finalizationService;
    }

    @Transactional
    public PracticeSubmission start(UUID ownerId, SubmissionStartCommand command) {
        if (ownerId == null || command == null || command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new SubmissionConflictException("Start idempotency key is required");
        }
        return repository.findByOwnerAndStartIdempotencyKey(ownerId, command.idempotencyKey().trim())
                .orElseGet(() -> create(ownerId, command));
    }

    public PracticeSubmission get(UUID ownerId, UUID submissionId) {
        return repository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission is not available"));
    }

    public SubmissionDraftSnapshot autosave(UUID ownerId, UUID submissionId, Map<String, String> payload,
            long expectedRevision, String idempotencyKey) {
        return draftService.autosave(new SubmissionDraftCommand(submissionId, ownerId, payload, expectedRevision, idempotencyKey));
    }

    public PracticeSubmission submit(UUID ownerId, UUID submissionId, Map<String, String> payload, String idempotencyKey) {
        return finalizationService.finalizeSubmission(ownerId, submissionId, payload, idempotencyKey);
    }

    private PracticeSubmission create(UUID ownerId, SubmissionStartCommand command) {
        ResolvedPracticeVersion resolved = practiceResolver.resolve(command.publishedSetId(), command.skill());
        Instant now = Instant.now();
        return repository.create(new PracticeSubmission(UUID.randomUUID(), ownerId, resolved.skill(),
                resolved.publishedSetId(), resolved.practiceVersionId(), resolved.publishedSetId(),
                resolved.publicationRevision(), SubmissionStatus.IN_PROGRESS, now, now, null, null, 0,
                command.idempotencyKey().trim(), null, null, false, now, now));
    }
}
