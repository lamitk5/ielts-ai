package com.ieltsaitutor.learning.intelligence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.attempt.AttemptLearningEventPublisher;
import com.ieltsaitutor.practice.attempt.AttemptLifecycleTest;
import com.ieltsaitutor.practice.attempt.AttemptService;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

class AttemptEventIdempotencyTest {
    @Test
    void retryingFinalPayloadDoesNotRepublishTheEvent() {
        AttemptLearningEventPublisher publisher = mock(AttemptLearningEventPublisher.class);
        AttemptService service = new AttemptService(new AttemptLifecycleTest.InMemoryAttemptRepository(), publisher);
        UUID userId = UUID.randomUUID();
        PracticeAttempt attempt = service.start(userId, "set", "v1", "reading", "retry-key");

        service.submit(userId, attempt.id(), Map.of("q1", "B"), 1, 1, "{}", "retry-key");
        service.submit(userId, attempt.id(), Map.of("q1", "B"), 1, 1, "{}", "retry-key");

        verify(publisher).publish(attempt, Map.of("q1", "B"), 1, 1);
    }
}
