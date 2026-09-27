package com.ieltsaitutor.practice.attempt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AttemptLifecycleTest {
    @Test
    void answersCanBeSavedOnlyBeforeSubmissionAndResultIsImmutable() {
        InMemoryAttemptRepository repository = new InMemoryAttemptRepository();
        AttemptService service = new AttemptService(repository);
        UUID user = UUID.randomUUID();
        PracticeAttempt attempt = service.start(user, "reading-foundation-01", "v1", "reading", "idempotency-1");

        service.saveAnswers(user, attempt.id(), Map.of("q1", "B"));
        PracticeAttempt submitted = service.submit(user, attempt.id(), Map.of("q1", "B"), 1, 1, "{}", "idempotency-1");

        assertEquals(AttemptStatus.FEEDBACK_READY, submitted.status());
        assertThrows(AttemptConflictException.class, () -> service.saveAnswers(user, attempt.id(), Map.of("q1", "A")));
    }

    @Test
    void duplicateFinalSubmissionReturnsSameResultButConflictingPayloadIsRejected() {
        InMemoryAttemptRepository repository = new InMemoryAttemptRepository();
        AttemptService service = new AttemptService(repository);
        UUID user = UUID.randomUUID();
        PracticeAttempt attempt = service.start(user, "reading-foundation-01", "v1", "reading", "idempotency-2");

        PracticeAttempt first = service.submit(user, attempt.id(), Map.of("q1", "B"), 1, 1, "{}", "idempotency-2");
        PracticeAttempt retry = service.submit(user, attempt.id(), Map.of("q1", "B"), 1, 1, "{}", "idempotency-2");

        assertEquals(first.id(), retry.id());
        assertThrows(AttemptConflictException.class, () -> service.submit(user, attempt.id(), Map.of("q1", "A"), 0, 1, "{}", "idempotency-2"));
    }

    @Test
    void attemptsAreOwnedAndBoundToTheirPracticeVersion() {
        InMemoryAttemptRepository repository = new InMemoryAttemptRepository();
        AttemptService service = new AttemptService(repository);
        UUID owner = UUID.randomUUID();
        PracticeAttempt attempt = service.start(owner, "reading-foundation-01", "version-7", "reading", "idempotency-3");

        assertEquals("version-7", service.get(owner, attempt.id()).practiceVersion());
        assertThrows(AttemptOwnershipException.class, () -> service.get(UUID.randomUUID(), attempt.id()));
    }

    static final class InMemoryAttemptRepository implements AttemptRepository {
        private final Map<UUID, PracticeAttempt> data = new HashMap<>();

        @Override
        public PracticeAttempt create(UUID userId, String practiceId, String practiceVersion, String skill, String idempotencyKey) {
            PracticeAttempt attempt = new PracticeAttempt(UUID.randomUUID(), userId, practiceId, practiceVersion, skill, AttemptStatus.IN_PROGRESS,
                    Map.of(), null, null, Instant.now(), null, null, idempotencyKey);
            data.put(attempt.id(), attempt);
            return attempt;
        }

        @Override public Optional<PracticeAttempt> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
        @Override public Optional<PracticeAttempt> findByUserAndIdempotencyKey(UUID userId, String idempotencyKey) {
            return data.values().stream().filter(item -> item.userId().equals(userId) && item.idempotencyKey().equals(idempotencyKey)).findFirst();
        }
        @Override public PracticeAttempt saveAnswers(PracticeAttempt attempt, Map<String, String> answers) {
            PracticeAttempt updated = attempt.withAnswers(answers);
            data.put(attempt.id(), updated);
            return updated;
        }
        @Override public PracticeAttempt saveResult(PracticeAttempt attempt, Map<String, String> answers, int score, int total, String resultPayload) {
            PracticeAttempt updated = attempt.withResult(answers, score, total, resultPayload);
            data.put(attempt.id(), updated);
            return updated;
        }
    }
}
