package com.ieltsaitutor.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.attempt.AttemptService;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;
import com.ieltsaitutor.practice.attempt.AttemptStatus;

class ReadingAttemptEndToEndTest {
    private SyntheticPracticeCatalog catalog;
    private AttemptService attempts;
    private PracticeService service;
    private UUID learner;

    @BeforeEach
    void setUp() {
        catalog = SyntheticPracticeCatalog.inMemory();
        attempts = org.mockito.Mockito.mock(AttemptService.class);
        service = new PracticeService(catalog, org.mockito.Mockito.mock(PracticeAttemptStore.class), attempts);
        learner = UUID.randomUUID();
    }

    @Test
    void readingAttemptBindsVersionPersistsAnswersAndScoresDeterministically() {
        UUID attemptId = UUID.randomUUID();
        PracticeAttempt started = new PracticeAttempt(attemptId, learner, "reading-foundation-01", "reading-foundation-01:v1",
                "reading", AttemptStatus.IN_PROGRESS, Map.of(), null, null, java.time.Instant.now(), null, "{}", "reading-key");
        when(attempts.start(eq(learner), eq("reading-foundation-01"), eq("reading-foundation-01:v1"), eq("reading"), eq("reading-key")))
                .thenReturn(started);
        when(attempts.get(learner, attemptId)).thenReturn(started);
        when(attempts.saveAnswers(learner, attemptId, Map.of("reading-q1", "B")))
                .thenReturn(started.withAnswers(Map.of("reading-q1", "B")));
        PracticeAttempt submitted = started.withResult(Map.of("reading-q1", "B", "reading-q2", "A"), 2, 2, "{}");
        when(attempts.submit(eq(learner), eq(attemptId), eq(Map.of("reading-q1", "B", "reading-q2", "A")), eq(2), eq(2), any(), eq("reading-key")))
                .thenReturn(submitted);

        PracticeAttempt actualStart = service.startReadingAttempt("reading-foundation-01", learner, "reading-key");
        PracticeAttempt saved = service.saveReadingAnswers(learner, attemptId, Map.of("reading-q1", "B"));
        PracticeAttempt actualSubmit = service.submitReadingAttempt(learner, attemptId,
                Map.of("reading-q1", "B", "reading-q2", "A"), "reading-key");

        assertEquals("reading-foundation-01:v1", actualStart.practiceVersion());
        assertEquals("B", saved.answers().get("reading-q1"));
        assertEquals(2, actualSubmit.score());
        assertEquals(2, actualSubmit.total());
        verify(attempts).submit(eq(learner), eq(attemptId), any(), eq(2), eq(2), any(), eq("reading-key"));
    }

    @Test
    void readingAttemptRejectsNonReadingContextAndKeepsAnswerKeysServerSide() {
        assertThrows(IllegalArgumentException.class,
                () -> service.readingResult(UUID.randomUUID(), UUID.randomUUID()));
        PracticeSet set = service.set("reading", "reading-foundation-01");
        assertNotNull(set.questions().get(0).answerKey());
        PracticeController.PracticeSetView publicView = PracticeController.PracticeSetView.from(set);
        assertNotNull(publicView.questions().get(0));
        assertEquals(2, publicView.questions().size());
    }
}
