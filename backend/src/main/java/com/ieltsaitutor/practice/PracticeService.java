package com.ieltsaitutor.practice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.practice.attempt.AttemptService;
import com.ieltsaitutor.practice.attempt.PracticeAttempt;

@Service
public class PracticeService {
    private final SyntheticPracticeCatalog catalog;
    private final PracticeAttemptStore attempts;
    private final AttemptService durableAttempts;

    public PracticeService(SyntheticPracticeCatalog catalog, PracticeAttemptStore attempts) {
        this(catalog, attempts, null);
    }

    @Autowired
    public PracticeService(SyntheticPracticeCatalog catalog, PracticeAttemptStore attempts, AttemptService durableAttempts) {
        this.catalog = catalog;
        this.attempts = attempts;
        this.durableAttempts = durableAttempts;
    }

    public List<PracticeSet> sets(String skill) { return catalog.sets(skill); }
    public PracticeSet set(String skill, String id) { return catalog.find(skill, id); }

    public PracticeAttemptResult submit(String skill, String setId, Map<String, String> answers, UUID userId) {
        PracticeSet set = catalog.find(skill, setId);
        int score = 0;
        List<PracticeReview> review = new ArrayList<>();
        for (PracticeQuestion question : set.questions()) {
            String selected = answers.getOrDefault(question.id(), "");
            boolean correct = question.answerKey().equalsIgnoreCase(selected.trim());
            if (correct) score++;
            review.add(new PracticeReview(question.id(), selected, question.answerKey(), correct, question.explanation()));
        }
        UUID attemptId = attempts.saveAndReturn(userId, set.skill().toLowerCase(), set.id(), score,
                set.questions().size(), answers);
        return new PracticeAttemptResult(attemptId, score, set.questions().size(), answers, review);
    }

    public PracticeAttempt startReadingAttempt(String setId, UUID userId, String idempotencyKey) {
        requireDurableAttempts();
        PracticeSet set = catalog.find("reading", setId);
        return durableAttempts.start(userId, set.id(), versionOf(set), "reading", idempotencyKey);
    }

    public PracticeAttempt startListeningAttempt(String setId, UUID userId, String idempotencyKey) {
        requireDurableAttempts();
        PracticeSet set = catalog.find("listening", setId);
        return durableAttempts.start(userId, set.id(), versionOf(set), "listening", idempotencyKey);
    }

    public PracticeAttempt saveReadingAnswers(UUID userId, UUID attemptId, Map<String, String> answers) {
        requireDurableAttempts();
        return durableAttempts.saveAnswers(userId, attemptId, answers == null ? Map.of() : answers);
    }

    public PracticeAttempt saveObjectiveAnswers(UUID userId, UUID attemptId, Map<String, String> answers) {
        requireDurableAttempts();
        return durableAttempts.saveAnswers(userId, attemptId, answers == null ? Map.of() : answers);
    }

    public PracticeAttempt submitReadingAttempt(UUID userId, UUID attemptId, Map<String, String> answers, String idempotencyKey) {
        requireDurableAttempts();
        PracticeAttempt attempt = durableAttempts.get(userId, attemptId);
        if (attempt == null) throw new IllegalArgumentException("Reading attempt not found");
        if (!"reading".equalsIgnoreCase(attempt.skill())) throw new IllegalArgumentException("Attempt is not a Reading attempt");
        PracticeSet set = catalog.find("reading", attempt.practiceId());
        Map<String, String> submittedAnswers = answers == null ? Map.of() : Map.copyOf(answers);
        int score = 0;
        for (PracticeQuestion question : set.questions()) {
            if (question.answerKey().equalsIgnoreCase(submittedAnswers.getOrDefault(question.id(), "").trim())) score++;
        }
        return durableAttempts.submit(userId, attemptId, submittedAnswers, score, set.questions().size(),
                resultPayload(set), idempotencyKey);
    }

    public PracticeAttempt readingResult(UUID userId, UUID attemptId) {
        requireDurableAttempts();
        PracticeAttempt attempt = durableAttempts.get(userId, attemptId);
        if (attempt == null) throw new IllegalArgumentException("Reading attempt not found");
        if (!"reading".equalsIgnoreCase(attempt.skill())) throw new IllegalArgumentException("Attempt is not a Reading attempt");
        return attempt;
    }

    public PracticeAttempt submitListeningAttempt(UUID userId, UUID attemptId, Map<String, String> answers, String idempotencyKey) {
        requireDurableAttempts();
        PracticeAttempt attempt = durableAttempts.get(userId, attemptId);
        if (attempt == null) throw new IllegalArgumentException("Listening attempt not found");
        if (!"listening".equalsIgnoreCase(attempt.skill())) throw new IllegalArgumentException("Attempt is not a Listening attempt");
        PracticeSet set = catalog.find("listening", attempt.practiceId());
        Map<String, String> submittedAnswers = answers == null ? Map.of() : Map.copyOf(answers);
        int score = 0;
        for (PracticeQuestion question : set.questions()) {
            if (question.answerKey().equalsIgnoreCase(submittedAnswers.getOrDefault(question.id(), "").trim())) score++;
        }
        return durableAttempts.submit(userId, attemptId, submittedAnswers, score, set.questions().size(),
                "{\"skill\":\"listening\",\"mediaStatus\":\"NOT_CONFIGURED\"}", idempotencyKey);
    }

    public PracticeAttempt listeningResult(UUID userId, UUID attemptId) {
        requireDurableAttempts();
        PracticeAttempt attempt = durableAttempts.get(userId, attemptId);
        if (attempt == null) throw new IllegalArgumentException("Listening attempt not found");
        if (!"listening".equalsIgnoreCase(attempt.skill())) throw new IllegalArgumentException("Attempt is not a Listening attempt");
        return attempt;
    }

    private String versionOf(PracticeSet set) { return set.id() + ":v1"; }

    private String resultPayload(PracticeSet set) {
        return "{\"skill\":\"reading\",\"questionCount\":" + set.questions().size()
                + ",\"passageReferences\":[\"" + escape(set.id()) + "\"]}";
    }

    private String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private void requireDurableAttempts() {
        if (durableAttempts == null) throw new IllegalStateException("Durable attempt service is not configured");
    }
}
