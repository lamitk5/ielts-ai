package com.ieltsaitutor.practice.attempt;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptService {
    private final AttemptRepository repository;
    private final AttemptLearningEventPublisher learningEventPublisher;

    public AttemptService(AttemptRepository repository) {
        this(repository, null);
    }

    @Autowired
    public AttemptService(AttemptRepository repository, AttemptLearningEventPublisher learningEventPublisher) {
        this.repository = repository;
        this.learningEventPublisher = learningEventPublisher;
    }

    public PracticeAttempt start(UUID userId, String practiceId, String practiceVersion, String skill, String idempotencyKey) {
        if (userId == null) throw new AttemptOwnershipException();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = repository.findByUserAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) return existing.get();
        }
        return repository.create(userId, practiceId, practiceVersion, skill, idempotencyKey);
    }

    public PracticeAttempt get(UUID userId, UUID attemptId) {
        return owned(userId, attemptId);
    }

    public PracticeAttempt saveAnswers(UUID userId, UUID attemptId, Map<String, String> answers) {
        PracticeAttempt attempt = owned(userId, attemptId);
        if (attempt.status() != AttemptStatus.IN_PROGRESS) throw new AttemptConflictException("Submitted attempts are immutable");
        return repository.saveAnswers(attempt, answers);
    }

    @Transactional
    public PracticeAttempt submit(UUID userId, UUID attemptId, Map<String, String> answers, int score, int total,
            String resultPayload, String idempotencyKey) {
        PracticeAttempt attempt = owned(userId, attemptId);
        if (attempt.status() != AttemptStatus.IN_PROGRESS) {
            if (attempt.answers().equals(answers) && java.util.Objects.equals(attempt.score(), score)
                    && java.util.Objects.equals(attempt.total(), total)
                    && java.util.Objects.equals(attempt.idempotencyKey(), idempotencyKey)) return attempt;
            throw new AttemptConflictException("Attempt has already been submitted");
        }
        if (attempt.idempotencyKey() != null && idempotencyKey != null && !attempt.idempotencyKey().equals(idempotencyKey)) {
            throw new AttemptConflictException("Submission key does not match attempt");
        }
        if (total <= 0 || score < 0 || score > total) throw new AttemptConflictException("Invalid result");
        if (learningEventPublisher != null) {
            learningEventPublisher.publish(attempt, answers, score, total);
        }
        return repository.saveResult(attempt, answers, score, total, resultPayload);
    }

    private PracticeAttempt owned(UUID userId, UUID attemptId) {
        return repository.findById(attemptId)
                .filter(attempt -> attempt.userId().equals(userId))
                .orElseThrow(AttemptOwnershipException::new);
    }
}
