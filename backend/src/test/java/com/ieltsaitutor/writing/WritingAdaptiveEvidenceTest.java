package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.LearningEvidencePipeline;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionCommandService;
import com.ieltsaitutor.submission.SubmissionStatus;

class WritingAdaptiveEvidenceTest {

    private WritingEvaluator evaluator;
    private WritingVersionRepository versionRepository;
    private WritingEvaluationRepository evaluationRepository;
    private PracticeSubmissionRepository submissionRepository;
    private SubmissionCommandService commandService;
    private LearningEvidencePipeline evidencePipeline;
    private WritingEvaluationService evaluationService;

    private UUID userId;
    private UUID submissionId;
    private UUID versionId;
    private PracticeSubmission submission;
    private WritingSubmissionVersion version;

    @BeforeEach
    void setUp() {
        evaluator = mock(WritingEvaluator.class);
        versionRepository = mock(WritingVersionRepository.class);
        evaluationRepository = mock(WritingEvaluationRepository.class);
        submissionRepository = mock(PracticeSubmissionRepository.class);
        commandService = mock(SubmissionCommandService.class);
        evidencePipeline = mock(LearningEvidencePipeline.class);

        evaluationService = new WritingEvaluationService(
                evaluator, versionRepository, evaluationRepository, submissionRepository, commandService, evidencePipeline);

        userId = UUID.randomUUID();
        submissionId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        submission = new PracticeSubmission(
                submissionId, userId, "WRITING", "task-2-essay", "v1", "set-2", 1,
                SubmissionStatus.SUBMITTED, Instant.now().minusSeconds(60), Instant.now(), Instant.now(),
                null, 1, null, "key2", "hash2", false, Instant.now().minusSeconds(60), Instant.now());

        version = new WritingSubmissionVersion(
                versionId, submissionId, 1, null, "Sample essay text with fifteen words here for testing purposes and verification now.",
                14, "hash2", Instant.now());

        when(submissionRepository.findByOwnerAndId(userId, submissionId)).thenReturn(Optional.of(submission));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(evaluationRepository.getNextEvaluationVersion(versionId)).thenReturn(1);
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Publishes adaptive learning event when evaluation is GRADED")
    void publishesEvidenceOnGraded() {
        WritingEvaluationResult gradedResult = new WritingEvaluationResult(
                UUID.randomUUID(), versionId, 1, 7.5,
                Map.of("taskResponse", "Good", "coherenceCohesion", "Good", "lexicalResource", "Good", "grammaticalRangeAccuracy", "Good"),
                List.of(), List.of(), List.of(), List.of(), List.of(), "NOT_ENABLED",
                WritingEvaluationResult.STANDARD_DISCLAIMER, "GRADED", Instant.now());

        when(evaluator.evaluate(any())).thenReturn(gradedResult);

        WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, versionId);

        assertEquals("GRADED", result.status());
        verify(evidencePipeline, times(1)).writingSubmitted(eq(userId), eq("task-2-essay"), eq(14), any(Instant.class));
    }

    @Test
    @DisplayName("Does NOT publish adaptive learning event when evaluation FAILS")
    void doesNotPublishEvidenceOnFailure() {
        WritingEvaluationResult failedResult = WritingEvaluationResult.failed(versionId, "Timeout");
        when(evaluator.evaluate(any())).thenReturn(failedResult);

        WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, versionId);

        assertEquals("FAILED", result.status());
        verify(evidencePipeline, never()).writingSubmitted(any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("Adaptive evidence pipeline exception does not break evaluation flow")
    void pipelineExceptionHandledSafely() {
        WritingEvaluationResult gradedResult = new WritingEvaluationResult(
                UUID.randomUUID(), versionId, 1, 7.0,
                Map.of("taskResponse", "Good", "coherenceCohesion", "Good", "lexicalResource", "Good", "grammaticalRangeAccuracy", "Good"),
                List.of(), List.of(), List.of(), List.of(), List.of(), "NOT_ENABLED",
                WritingEvaluationResult.STANDARD_DISCLAIMER, "GRADED", Instant.now());

        when(evaluator.evaluate(any())).thenReturn(gradedResult);
        doThrow(new RuntimeException("Adaptive pipeline unavailable"))
                .when(evidencePipeline).writingSubmitted(any(), any(), anyInt(), any());

        assertDoesNotThrow(() -> {
            WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, versionId);
            assertEquals("GRADED", result.status());
        });
    }
}
