package com.ieltsaitutor.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.PracticeAttemptStore;
import com.ieltsaitutor.practice.PracticeService;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.practice.attempt.AttemptService;
import com.ieltsaitutor.practice.attempt.AttemptStatus;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

class StudentReadyLearnerFlowE2ETest {
    @Test
    void learnerCanResumeAnswerSubmitAndReadDeterministicReadingResult() {
        UUID learner = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        AttemptService attempts = mock(AttemptService.class);
        PracticeService practice = new PracticeService(SyntheticPracticeCatalog.inMemory(), mock(PracticeAttemptStore.class), attempts);
        PracticeAttempt started = new PracticeAttempt(attemptId, learner, "reading-foundation-01", "reading-foundation-01:v1",
                "reading", AttemptStatus.IN_PROGRESS, Map.of(), null, null, Instant.now(), null, "{}", "qa-key");
        PracticeAttempt answered = started.withAnswers(Map.of("reading-q1", "B"));
        PracticeAttempt result = answered.withResult(Map.of("reading-q1", "B", "reading-q2", "A"), 2, 2, "{\"skill\":\"reading\"}");
        when(attempts.start(eq(learner), eq("reading-foundation-01"), eq("reading-foundation-01:v1"), eq("reading"), eq("qa-key"))).thenReturn(started);
        when(attempts.get(learner, attemptId)).thenReturn(answered, result);
        when(attempts.saveAnswers(learner, attemptId, Map.of("reading-q1", "B"))).thenReturn(answered);
        when(attempts.submit(eq(learner), eq(attemptId), any(), eq(2), eq(2), any(), eq("qa-key"))).thenReturn(result);

        assertEquals(attemptId, practice.startReadingAttempt("reading-foundation-01", learner, "qa-key").id());
        assertEquals("B", practice.saveReadingAnswers(learner, attemptId, Map.of("reading-q1", "B")).answers().get("reading-q1"));
        assertEquals(2, practice.submitReadingAttempt(learner, attemptId, Map.of("reading-q1", "B", "reading-q2", "A"), "qa-key").score());
        assertEquals(2, practice.readingResult(learner, attemptId).total());
    }
}
