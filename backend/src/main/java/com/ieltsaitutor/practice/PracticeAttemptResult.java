package com.ieltsaitutor.practice;

import java.util.List;
import java.util.UUID;

public record PracticeAttemptResult(UUID attemptId, int score, int total, java.util.Map<String, String> answers,
        List<PracticeReview> review) {
    public PracticeAttemptResult(int score, int total, java.util.Map<String, String> answers,
            List<PracticeReview> review) {
        this(null, score, total, answers, review);
    }
}
