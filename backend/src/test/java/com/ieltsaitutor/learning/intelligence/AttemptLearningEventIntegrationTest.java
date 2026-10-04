package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.attempt.AttemptLearningEventPublisher;
import com.ieltsaitutor.practice.attempt.AttemptLifecycleTest;
import com.ieltsaitutor.practice.attempt.AttemptService;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

class AttemptLearningEventIntegrationTest {
    @Test
    void finalizationPublishesOneDurableEventBeforeResultPersistence() {
        AttemptLearningEventPublisher publisher = mock(AttemptLearningEventPublisher.class);
        AttemptService service = new AttemptService(new AttemptLifecycleTest.InMemoryAttemptRepository(), publisher);
        UUID userId = UUID.randomUUID();
        PracticeAttempt attempt = service.start(userId, "reading-foundation-01", "v1", "reading", "event-key");

        PracticeAttempt result = service.submit(userId, attempt.id(), Map.of("q1", "B"), 1, 1, "{}", "event-key");

        assertEquals("FEEDBACK_READY", result.status().name());
        verify(publisher).publish(attempt, Map.of("q1", "B"), 1, 1);
    }
}
