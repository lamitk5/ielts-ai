package com.ieltsaitutor.writing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.learning.intelligence.LearningEvidencePipeline;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionCommand;
import com.ieltsaitutor.submission.SubmissionCommandService;
import com.ieltsaitutor.submission.SubmissionConflictException;
import com.ieltsaitutor.submission.SubmissionStatus;

@Service
public class WritingEvaluationService {

    private final WritingEvaluator evaluator;
    private final WritingVersionRepository versionRepository;
    private final WritingEvaluationRepository evaluationRepository;
    private final PracticeSubmissionRepository submissionRepository;
    private final SubmissionCommandService commandService;
    private final LearningEvidencePipeline evidencePipeline;

    @Autowired
    public WritingEvaluationService(
            WritingEvaluator evaluator,
            WritingVersionRepository versionRepository,
            WritingEvaluationRepository evaluationRepository,
            PracticeSubmissionRepository submissionRepository,
            SubmissionCommandService commandService,
            @Autowired(required = false) LearningEvidencePipeline evidencePipeline) {
        this.evaluator = evaluator;
        this.versionRepository = versionRepository;
        this.evaluationRepository = evaluationRepository;
        this.submissionRepository = submissionRepository;
        this.commandService = commandService;
        this.evidencePipeline = evidencePipeline;
    }

    public WritingEvaluationService(
            WritingEvaluator evaluator,
            WritingVersionRepository versionRepository,
            WritingEvaluationRepository evaluationRepository,
            PracticeSubmissionRepository submissionRepository,
            SubmissionCommandService commandService) {
        this(evaluator, versionRepository, evaluationRepository, submissionRepository, commandService, null);
    }

    @Transactional
    public WritingSubmissionVersion createVersion(UUID ownerId, UUID submissionId, String responseText, UUID parentVersionId) {
        PracticeSubmission submission = submissionRepository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Writing submission not found or access denied"));

        int nextVersionNumber = versionRepository.getNextVersionNumber(submissionId);
        int wordCount = calculateWordCount(responseText);
        String hash = computeSha256(responseText);

        WritingSubmissionVersion version = new WritingSubmissionVersion(
                UUID.randomUUID(),
                submissionId,
                nextVersionNumber,
                parentVersionId,
                responseText != null ? responseText : "",
                wordCount,
                hash,
                Instant.now());

        return versionRepository.save(version);
    }

    @Transactional
    public WritingEvaluationResult evaluateVersion(UUID ownerId, UUID submissionId, UUID versionId) {
        PracticeSubmission submission = submissionRepository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Writing submission not found or access denied"));

        WritingSubmissionVersion version = versionRepository.findById(versionId)
                .filter(v -> v.submissionId().equals(submissionId))
                .orElseThrow(() -> new SubmissionConflictException("Writing version not found"));

        WritingTaskRubric rubric = WritingRubricPolicy.resolveRubric(submission.practiceId());

        // Transition submission to AI_EVALUATING if needed
        if (submission.status() == SubmissionStatus.SUBMITTED) {
            commandService.transition(ownerId, submissionId, SubmissionCommand.BEGIN_AI_EVALUATION);
        }

        WritingEvaluationCommand command = new WritingEvaluationCommand(
                submissionId,
                versionId,
                submission.practiceId(),
                version.responseText(),
                rubric);

        WritingEvaluationResult result = evaluator.evaluate(command);
        int nextEvalVersion = evaluationRepository.getNextEvaluationVersion(versionId);

        WritingEvaluationResult toPersist = new WritingEvaluationResult(
                UUID.randomUUID(),
                versionId,
                nextEvalVersion,
                result.overallBandEstimate(),
                result.criteria(),
                result.strengths(),
                result.issues(),
                result.suggestions(),
                result.evidenceSpans(),
                result.priorityImprovements(),
                result.groundingStatus(),
                result.disclaimer(),
                result.status(),
                Instant.now());

        WritingEvaluationResult saved = evaluationRepository.save(toPersist);

        if ("GRADED".equals(saved.status())) {
            commandService.transition(ownerId, submissionId, SubmissionCommand.GRADE);
            publishEvidenceSafely(ownerId, submission.practiceId(), version.wordCount(), saved);
        } else {
            commandService.transition(ownerId, submissionId, SubmissionCommand.FAIL);
        }

        return saved;
    }

    @Transactional
    public WritingEvaluationResult retryEvaluation(UUID ownerId, UUID submissionId, UUID versionId) {
        PracticeSubmission submission = submissionRepository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Writing submission not found or access denied"));

        if (submission.status() == SubmissionStatus.FAILED) {
            commandService.retry(ownerId, submissionId, SubmissionStatus.AI_EVALUATING);
        }
        return evaluateVersion(ownerId, submissionId, versionId);
    }

    public List<WritingSubmissionVersion> getVersions(UUID ownerId, UUID submissionId) {
        submissionRepository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Writing submission not found or access denied"));
        return versionRepository.findBySubmissionId(submissionId);
    }

    public Optional<WritingEvaluationResult> getLatestEvaluation(UUID ownerId, UUID submissionId, UUID versionId) {
        submissionRepository.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Writing submission not found or access denied"));
        return evaluationRepository.findLatestByVersionId(versionId);
    }

    private void publishEvidenceSafely(UUID ownerId, String taskId, int wordCount, WritingEvaluationResult evaluation) {
        if (evidencePipeline == null || !"GRADED".equals(evaluation.status())) {
            return;
        }
        try {
            evidencePipeline.writingSubmitted(ownerId, taskId, wordCount, Instant.now());
        } catch (RuntimeException ignored) {
            // Adaptive evidence failure must never fail the submission or roll back grading
        }
    }

    private int calculateWordCount(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((input != null ? input : "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
