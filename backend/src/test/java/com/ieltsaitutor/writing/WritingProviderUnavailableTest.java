package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionCommand;
import com.ieltsaitutor.submission.SubmissionCommandService;
import com.ieltsaitutor.submission.SubmissionStatus;

class WritingProviderUnavailableTest {

    private AiProvider aiProvider;
    private WritingVersionRepository versionRepository;
    private WritingEvaluationRepository evaluationRepository;
    private PracticeSubmissionRepository submissionRepository;
    private SubmissionCommandService commandService;
    private WritingEvaluationService evaluationService;

    private UUID userId;
    private UUID submissionId;
    private WritingSubmissionVersion version;

    @BeforeEach
    void setUp() {
        aiProvider = mock(AiProvider.class);
        versionRepository = mock(WritingVersionRepository.class);
        evaluationRepository = mock(WritingEvaluationRepository.class);
        submissionRepository = mock(PracticeSubmissionRepository.class);
        commandService = mock(SubmissionCommandService.class);

        WritingEvaluationValidator validator = new WritingEvaluationValidator();
        WritingEvaluator evaluator = new WritingEvaluator(aiProvider, validator);

        evaluationService = new WritingEvaluationService(
                evaluator, versionRepository, evaluationRepository, submissionRepository, commandService, null);

        userId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        PracticeSubmission submission = new PracticeSubmission(
                submissionId, userId, "WRITING", "task-2-opinion-01", "v1", "set-2", 1,
                SubmissionStatus.SUBMITTED, Instant.now().minusSeconds(120), Instant.now(), Instant.now(),
                null, 1, null, "key2", "hash2", false, Instant.now().minusSeconds(120), Instant.now());

        version = new WritingSubmissionVersion(
                UUID.randomUUID(), submissionId, 1, null,
                "Some people believe that university education should be free for all students. In my opinion, I agree with this statement.",
                21, "hash2", Instant.now());

        when(submissionRepository.findByOwnerAndId(userId, submissionId)).thenReturn(Optional.of(submission));
        when(versionRepository.findById(version.id())).thenReturn(Optional.of(version));
        when(evaluationRepository.getNextEvaluationVersion(version.id())).thenReturn(1);
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Provider HTTP 429 / rate limit / timeout marks evaluation FAILED safely without destroying version text")
    void providerFailurePreservesVersion() {
        when(aiProvider.chat(any())).thenThrow(new RuntimeException("Rate limit 429: quota exhausted"));

        WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, version.id());

        assertEquals("FAILED", result.status());
        assertNull(result.overallBandEstimate());
        verify(commandService).transition(userId, submissionId, SubmissionCommand.FAIL);
        // Original text is preserved in repository
        assertEquals("Some people believe that university education should be free for all students. In my opinion, I agree with this statement.", version.responseText());
    }

    @Test
    @DisplayName("Malformed AI output marks evaluation MALFORMED and fails submission safely")
    void malformedOutputFailsSafely() {
        when(aiProvider.chat(any())).thenReturn(AiChatResult.answered("Sorry I cannot assist with this prompt."));

        WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, version.id());

        assertEquals("MALFORMED", result.status());
        assertNull(result.overallBandEstimate());
        verify(commandService).transition(userId, submissionId, SubmissionCommand.FAIL);
    }
}
