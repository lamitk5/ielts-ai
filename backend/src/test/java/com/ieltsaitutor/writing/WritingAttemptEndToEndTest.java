package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.model.AiChatResult;
import com.ieltsaitutor.ai.provider.AiProvider;

class WritingAttemptEndToEndTest {
    @Test
    void taskOneAndTaskTwoKeepCriteriaAndPersistTruthfulAttemptResult() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.chat(any())).thenReturn(AiChatResult.answered("""
                {"overallBandEstimate":6.5,"criteria":{"Task response":"6.5","Coherence":"6.0","Lexical resource":"6.5","Grammar":"6.0"}}
                """));
        WritingRepository repository = mock(WritingRepository.class);
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        WritingAttempt attempt = new WritingAttempt(attemptId, userId, "task-1-academic-01", "TASK_1", "IN_PROGRESS", "", 0, Instant.now(), null, null);
        when(repository.start(eq(userId), eq("task-1-academic-01"))).thenReturn(attempt);
        when(repository.find(attemptId, userId)).thenReturn(java.util.Optional.of(attempt));
        when(repository.complete(any(), any())).thenAnswer(invocation -> {
            WritingAttempt draft = invocation.getArgument(0);
            return new WritingAttempt(draft.id(), draft.userId(), draft.taskId(), draft.taskType(), "FEEDBACK_READY",
                    draft.responseText(), draft.wordCount(), draft.createdAt(), Instant.now(), invocation.getArgument(1));
        });

        WritingAssessmentService service = new WritingAssessmentService(provider, repository);
        WritingAttempt started = service.startAttempt(userId, "task-1-academic-01");
        WritingAttempt submitted = service.submitAttempt(userId, attemptId, "A sufficiently long response with clear ideas and supporting examples for the academic task.");

        assertEquals("TASK_1", started.taskType());
        assertEquals("ANSWERED", submitted.assessment().status());
        assertEquals(6.5, submitted.assessment().overallBandEstimate());
        verify(repository).complete(any(), any());
    }
}
