package com.ieltsaitutor.practice.attempt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AttemptSubmissionIdempotencyTest {
    @Test
    void ownershipFailureDoesNotRevealAttemptState() {
        AttemptRepository repository = new AttemptLifecycleTest.InMemoryAttemptRepository();
        AttemptService service = new AttemptService(repository);
        UUID owner = UUID.randomUUID();
        PracticeAttempt attempt = service.start(owner, "set", "version", "reading", "key");

        AttemptOwnershipException error = assertThrows(AttemptOwnershipException.class,
                () -> service.submit(UUID.randomUUID(), attempt.id(), java.util.Map.of(), 0, 1, "{}", "key"));
        assertEquals("Attempt is not available", error.getMessage());
    }
}
