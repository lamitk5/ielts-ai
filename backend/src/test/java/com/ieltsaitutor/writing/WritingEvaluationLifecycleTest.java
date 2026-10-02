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

import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionCommand;
import com.ieltsaitutor.submission.SubmissionCommandService;
import com.ieltsaitutor.submission.SubmissionStatus;

class WritingEvaluationLifecycleTest {

    private WritingEvaluator evaluator;
    private WritingVersionRepository versionRepository;
    private WritingEvaluationRepository evaluationRepository;
    private PracticeSubmissionRepository submissionRepository;
    private SubmissionCommandService commandService;
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
        evaluationService = new WritingEvaluationService(
                evaluator, versionRepository, evaluationRepository, submissionRepository, commandService, null);

        userId = UUID.randomUUID();
        submissionId = UUID.randomUUID();
        versionId = UUID.randomUUID();

        submission = new PracticeSubmission(
                submissionId, userId, "WRITING", "task-1-bar-chart", "v1", "set-1", 1,
                SubmissionStatus.SUBMITTED, Instant.now().minusSeconds(60), Instant.now(), Instant.now(),
                null, 1, null, "key1", "hash1", false, Instant.now().minusSeconds(60), Instant.now());

        version = new WritingSubmissionVersion(
                versionId, submissionId, 1, null, "My essay response text", 5, "hash1", Instant.now());
    }

    @Test
    @DisplayName("Successful AI evaluation grades the submission and saves evaluation")
    void successfulEvaluationGradesSubmission() {
        when(submissionRepository.findByOwnerAndId(userId, submissionId)).thenReturn(Optional.of(submission));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(evaluationRepository.getNextEvaluationVersion(versionId)).thenReturn(1);

        WritingEvaluationResult expectedResult = new WritingEvaluationResult(
                UUID.randomUUID(), versionId, 1, 7.0,
                Map.of("taskAchievement", "Good", "coherenceCohesion", "Good", "lexicalResource", "Good", "grammaticalRangeAccuracy", "Good"),
                List.of("Strong overview"), List.of(), List.of(), List.of(), List.of(), "NOT_ENABLED",
                WritingEvaluationResult.STANDARD_DISCLAIMER, "GRADED", Instant.now());

        when(evaluator.evaluate(any())).thenReturn(expectedResult);
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, versionId);

        assertEquals("GRADED", result.status());
        assertEquals(7.0, result.overallBandEstimate());
        verify(commandService).transition(userId, submissionId, SubmissionCommand.BEGIN_AI_EVALUATION);
        verify(commandService).transition(userId, submissionId, SubmissionCommand.GRADE);
        verify(evaluationRepository).save(any());
    }

    @Test
    @DisplayName("AI timeout / failure marks submission FAILED without destroying text")
    void aiFailureMarksSubmissionFailed() {
        when(submissionRepository.findByOwnerAndId(userId, submissionId)).thenReturn(Optional.of(submission));
        when(versionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(evaluationRepository.getNextEvaluationVersion(versionId)).thenReturn(1);

        WritingEvaluationResult failedResult = WritingEvaluationResult.failed(versionId, "AI Timeout");
        when(evaluator.evaluate(any())).thenReturn(failedResult);
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, versionId);

        assertEquals("FAILED", result.status());
        verify(commandService).transition(userId, submissionId, SubmissionCommand.BEGIN_AI_EVALUATION);
        verify(commandService).transition(userId, submissionId, SubmissionCommand.FAIL);
        verify(evaluationRepository).save(any());
    }
}
