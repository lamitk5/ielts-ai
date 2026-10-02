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

class WritingAttemptEndToEndTest {

    private AiProvider aiProvider;
    private WritingVersionRepository versionRepository;
    private WritingEvaluationRepository evaluationRepository;
    private PracticeSubmissionRepository submissionRepository;
    private SubmissionCommandService commandService;
    private WritingEvaluationService evaluationService;

    private UUID userId;
    private UUID submissionId;

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
    }

    @Test
    @DisplayName("End-to-end Writing Task 1 flow with mocked AI provider")
    void task1EndToEndFlow() {
        PracticeSubmission submission = new PracticeSubmission(
                submissionId, userId, "WRITING", "task-1-bar-chart", "v1", "set-1", 1,
                SubmissionStatus.SUBMITTED, Instant.now().minusSeconds(120), Instant.now(), Instant.now(),
                null, 1, null, "key1", "hash1", false, Instant.now().minusSeconds(120), Instant.now());

        WritingSubmissionVersion version = new WritingSubmissionVersion(
                UUID.randomUUID(), submissionId, 1, null,
                "The chart shows the consumption of energy in five countries from 2000 to 2020. Overall, energy consumption increased significantly.",
                21, "hash1", Instant.now());

        when(submissionRepository.findByOwnerAndId(userId, submissionId)).thenReturn(Optional.of(submission));
        when(versionRepository.findById(version.id())).thenReturn(Optional.of(version));
        when(evaluationRepository.getNextEvaluationVersion(version.id())).thenReturn(1);
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String validAiResponse = """
                {
                  "overallBandEstimate": 7.0,
                  "criteria": {
                    "taskAchievement": "Presents a clear overview with major trends",
                    "coherenceCohesion": "Information is logically organized",
                    "lexicalResource": "Uses a sufficient range of vocabulary",
                    "grammaticalRangeAccuracy": "Good mix of sentence structures"
                  },
                  "strengths": ["Clear overview paragraph"],
                  "issues": ["Minor data omission for 2010"],
                  "suggestions": ["Include specific figures for 2010"],
                  "evidenceSpans": ["energy consumption increased significantly"],
                  "priorityImprovements": ["Ensure all key years are addressed"]
                }
                """;

        when(aiProvider.chat(any())).thenReturn(AiChatResult.answered(validAiResponse));

        WritingEvaluationResult result = evaluationService.evaluateVersion(userId, submissionId, version.id());

        assertEquals("GRADED", result.status());
        assertEquals(7.0, result.overallBandEstimate());
        assertEquals("Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.", result.disclaimer());
        assertTrue(result.criteria().containsKey("taskAchievement"));
        assertFalse(result.criteria().containsKey("taskResponse"));
        verify(commandService).transition(userId, submissionId, SubmissionCommand.BEGIN_AI_EVALUATION);
        verify(commandService).transition(userId, submissionId, SubmissionCommand.GRADE);
    }
}
