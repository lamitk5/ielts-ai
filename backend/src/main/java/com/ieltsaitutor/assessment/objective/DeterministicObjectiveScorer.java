package com.ieltsaitutor.assessment.objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.PracticeQuestion;

@Component
public class DeterministicObjectiveScorer {
    private final ObjectiveScoringPolicy policy;

    public DeterministicObjectiveScorer() { this(ObjectiveScoringPolicy.current()); }
    public DeterministicObjectiveScorer(ObjectiveScoringPolicy policy) { this.policy = policy; }

    public ObjectiveScore score(List<PracticeQuestion> questions, Map<String, String> learnerAnswers) {
        List<PracticeQuestion> items = questions == null ? List.of() : questions;
        Map<String, String> answers = learnerAnswers == null ? Map.of() : learnerAnswers;
        List<QuestionScore> results = new ArrayList<>();
        int correct = 0;
        for (PracticeQuestion question : items) {
            String type = question.questionType();
            String learnerAnswer = answers.getOrDefault(question.id(), "");
            String normalizedLearner = AnswerNormalizer.normalize(learnerAnswer, type);
            String normalizedKey = AnswerNormalizer.normalize(question.answerKey(), type);
            boolean isCorrect = equivalent(question, normalizedLearner, normalizedKey);
            if (isCorrect) correct++;
            results.add(new QuestionScore(question.id(), type, learnerAnswer, normalizedLearner,
                    question.answerKey(), isCorrect, question.evidenceReference(), question.explanation()));
        }
        return new ObjectiveScore(correct, items.size(), null, policy.version(), results);
    }

    private boolean equivalent(PracticeQuestion question, String learner, String key) {
        if (learner.isEmpty() || key.isEmpty()) return false;
        if (learner.equals(key)) return true;
        if ("MULTIPLE_CHOICE".equalsIgnoreCase(question.questionType()) && question.options() != null) {
            for (int i = 0; i < question.options().size(); i++) {
                String letter = String.valueOf((char) ('A' + i));
                if (key.equalsIgnoreCase(letter) && learner.equals(AnswerNormalizer.normalize(question.options().get(i), "SHORT_ANSWER"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
