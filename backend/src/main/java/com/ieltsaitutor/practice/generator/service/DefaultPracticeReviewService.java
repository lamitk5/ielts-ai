package com.ieltsaitutor.practice.generator.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeSet;
import com.ieltsaitutor.practice.generator.domain.GeneratedPracticeVersion;
import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.domain.GenerationValidationResult;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationBlueprint;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationJob;
import com.ieltsaitutor.practice.generator.domain.PracticeGenerationSource;
import com.ieltsaitutor.practice.generator.domain.PracticeReviewAction;
import com.ieltsaitutor.practice.generator.dto.GeneratedSetReviewPayload;
import com.ieltsaitutor.practice.generator.dto.ReviewActionRequest;
import com.ieltsaitutor.practice.generator.dto.ReviewActionResponse;
import com.ieltsaitutor.practice.generator.exception.InvalidReviewActionException;
import com.ieltsaitutor.practice.generator.repository.PracticeGenerationRepository;

@Service
public class DefaultPracticeReviewService implements PracticeReviewService {

    private final PracticeGenerationRepository repository;
    private final PracticeAuditService auditService;
    private final PracticeBankHydrationService hydrationService;
    private final GenerationStateMachine stateMachine;

    public DefaultPracticeReviewService(
            PracticeGenerationRepository repository,
            PracticeAuditService auditService,
            PracticeBankHydrationService hydrationService,
            GenerationStateMachine stateMachine) {
        this.repository = repository;
        this.auditService = auditService;
        this.hydrationService = hydrationService;
        this.stateMachine = stateMachine;
    }

    @Override
    @Transactional
    public ReviewActionResponse executeReview(UUID setId, ReviewActionRequest request, UUID adminId) {
        GeneratedPracticeSet set = repository.findSetById(setId)
                .orElseThrow(() -> new IllegalArgumentException("Generated practice set not found: " + setId));

        String rawAction = request.action().toUpperCase();

        if ("APPROVE".equals(rawAction)) {
            if (!stateMachine.canTransition(set.state(), GenerationState.APPROVED)) {
                throw new InvalidReviewActionException("Cannot approve set in state: " + set.state());
            }

            GeneratedPracticeVersion currentVersion = set.currentVersionId() != null
                    ? repository.findVersionById(set.currentVersionId()).orElse(null)
                    : null;

            String publishedSetId = hydrationService.hydrate(set, currentVersion, adminId);
            repository.markSetApproved(setId, publishedSetId, adminId);
            PracticeReviewAction audit = auditService.recordAction(
                    setId, set.currentVersionId(), adminId, "APPROVE",
                    request.feedbackNotes() != null ? request.feedbackNotes() : "Approved by administrator"
            );

            return new ReviewActionResponse(
                    audit.id(), setId, "APPROVE", GenerationState.APPROVED,
                    adminId, audit.createdAt(),
                    "Practice set approved and published as " + publishedSetId
            );
        } else if ("REQUEST_REVISION".equals(rawAction)) {
            if (!stateMachine.canTransition(set.state(), GenerationState.NEEDS_REVISION)) {
                throw new InvalidReviewActionException("Cannot request revision for set in state: " + set.state());
            }

            repository.updateSetState(setId, GenerationState.NEEDS_REVISION, set.currentVersionId());
            String notes = (request.revisionInstructions() != null && !request.revisionInstructions().isBlank())
                    ? request.revisionInstructions()
                    : request.feedbackNotes();
            PracticeReviewAction audit = auditService.recordAction(
                    setId, set.currentVersionId(), adminId, "REQUEST_REVISION", notes
            );

            return new ReviewActionResponse(
                    audit.id(), setId, "REQUEST_REVISION", GenerationState.NEEDS_REVISION,
                    adminId, audit.createdAt(),
                    "Revision requested: " + (notes != null ? notes : "")
            );
        } else if ("REJECT".equals(rawAction)) {
            if (!stateMachine.canTransition(set.state(), GenerationState.REJECTED)) {
                throw new InvalidReviewActionException("Cannot reject set in state: " + set.state());
            }

            repository.updateSetState(setId, GenerationState.REJECTED, set.currentVersionId());
            PracticeReviewAction audit = auditService.recordAction(
                    setId, set.currentVersionId(), adminId, "REJECT",
                    request.feedbackNotes() != null ? request.feedbackNotes() : "Rejected by administrator"
            );

            return new ReviewActionResponse(
                    audit.id(), setId, "REJECT", GenerationState.REJECTED,
                    adminId, audit.createdAt(),
                    "Practice set rejected"
            );
        } else {
            throw new InvalidReviewActionException("Unsupported review action: " + request.action());
        }
    }

    @Override
    public GeneratedSetReviewPayload getReviewPayload(UUID setId) {
        GeneratedPracticeSet set = repository.findSetById(setId)
                .orElseThrow(() -> new IllegalArgumentException("Generated practice set not found: " + setId));

        GeneratedPracticeVersion currentVersion = set.currentVersionId() != null
                ? repository.findVersionById(set.currentVersionId()).orElse(null)
                : null;

        PracticeGenerationJob job = set.jobId() != null
                ? repository.findJobById(set.jobId()).orElse(null)
                : null;

        PracticeGenerationBlueprint blueprint = (job != null && job.blueprintId() != null)
                ? repository.findBlueprintById(job.blueprintId()).orElse(null)
                : null;

        PracticeGenerationSource source = (job != null && job.sourceId() != null)
                ? repository.findSourceById(job.sourceId()).orElse(null)
                : null;

        List<GenerationValidationResult> validationResults = currentVersion != null
                ? repository.listValidationResultsForVersion(currentVersion.id())
                : List.of();

        List<PracticeReviewAction> reviewHistory = repository.listReviewActionsForSet(setId);
        List<GeneratedPracticeVersion> versionHistory = repository.listVersionsForSet(setId);

        return new GeneratedSetReviewPayload(
                set, currentVersion, blueprint, source, validationResults, reviewHistory, versionHistory
        );
    }

    @Override
    public List<GeneratedPracticeSet> listSets(String stateFilter) {
        GenerationState state = null;
        if (stateFilter != null && !stateFilter.isBlank()) {
            state = GenerationState.valueOf(stateFilter.toUpperCase());
        }
        return repository.listSets(state, 100, 0);
    }

    @Override
    public List<GeneratedPracticeVersion> getVersionHistory(UUID setId) {
        return repository.listVersionsForSet(setId);
    }

    @Override
    public List<PracticeReviewAction> getAuditHistory(UUID setId) {
        return repository.listReviewActionsForSet(setId);
    }
}
