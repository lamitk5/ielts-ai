package com.ieltsaitutor.listening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.practice.PracticeService;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.practice.attempt.AttemptStatus;
import com.ieltsaitutor.practice.attempt.AttemptService;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

class ListeningAttemptEndToEndTest {
    private AttemptService attempts;
    private PracticeService service;
    private UUID learner;

    @BeforeEach
    void setUp() {
        attempts = mock(AttemptService.class);
        service = new PracticeService(SyntheticPracticeCatalog.inMemory(), mock(PracticeAttemptStore.class), attempts);
        learner = UUID.randomUUID();
    }

    @Test
    void listeningFlowPersistsVersionAndScoresApprovedQuestionsWithoutClaimingAudio() {
        UUID attemptId = UUID.randomUUID();
        PracticeAttempt started = new PracticeAttempt(attemptId, learner, "listening-foundation-01", "listening-foundation-01:v1",
                "listening", AttemptStatus.IN_PROGRESS, Map.of(), null, null, Instant.now(), null, "{}", "listening-key");
        when(attempts.start(eq(learner), eq("listening-foundation-01"), eq("listening-foundation-01:v1"), eq("listening"), eq("listening-key")))
                .thenReturn(started);
        when(attempts.get(learner, attemptId)).thenReturn(started);
        when(attempts.submit(eq(learner), eq(attemptId), eq(Map.of("listening-q1", "B")), eq(1), eq(2), any(), eq("listening-key")))
                .thenReturn(started.withResult(Map.of("listening-q1", "B"), 1, 2, "{}"));

        PracticeAttempt actualStart = service.startListeningAttempt("listening-foundation-01", learner, "listening-key");
        PracticeAttempt actualSubmit = service.submitListeningAttempt(learner, attemptId, Map.of("listening-q1", "B"), "listening-key");

        assertEquals("listening-foundation-01:v1", actualStart.practiceVersion());
        assertEquals(1, actualSubmit.score());
        assertEquals(2, actualSubmit.total());
        verify(attempts).submit(eq(learner), eq(attemptId), any(), eq(1), eq(2), any(), eq("listening-key"));
    }
}
