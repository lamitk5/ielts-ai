package com.ieltsaitutor.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.attempt.AttemptRepository;
import com.ieltsaitutor.practice.attempt.AttemptService;
import com.ieltsaitutor.practice.attempt.AttemptStatus;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

class StudentReadyRestartE2ETest {
    @Test
    void aFreshServiceInstanceReadsThePersistedFinalResult() {
        UUID learner = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        PracticeAttempt finalResult = new PracticeAttempt(attemptId, learner, "reading-foundation-01", "reading-foundation-01:v1",
                "reading", AttemptStatus.FEEDBACK_READY, Map.of("reading-q1", "B"), 1, 2, Instant.now(), Instant.now(), "{\"score\":1}", "qa-key");
        AttemptRepository repository = mock(AttemptRepository.class);
        when(repository.findById(attemptId)).thenReturn(Optional.of(finalResult));

        PracticeAttempt restored = new AttemptService(repository).get(learner, attemptId);

        assertEquals(AttemptStatus.FEEDBACK_READY, restored.status());
        assertEquals(1, restored.score());
        assertEquals("{\"score\":1}", restored.resultPayload());
    }
}
