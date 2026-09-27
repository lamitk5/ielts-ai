package com.ieltsaitutor.practice.attempt;

import java.util.Map;

public interface AttemptLearningEventPublisher {
    void publish(PracticeAttempt attempt, Map<String, String> answers, int score, int total);
}
