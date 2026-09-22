package com.ieltsaitutor.practice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class PracticeService {
    private final SyntheticPracticeCatalog catalog;
    private final PracticeAttemptStore attempts;

    public PracticeService(SyntheticPracticeCatalog catalog, PracticeAttemptStore attempts) {
        this.catalog = catalog;
        this.attempts = attempts;
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
        attempts.save(userId, set.skill().toLowerCase(), set.id(), score, set.questions().size(), answers);
        return new PracticeAttemptResult(score, set.questions().size(), answers, review);
    }
}
