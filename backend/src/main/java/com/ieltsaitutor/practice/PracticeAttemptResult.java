package com.ieltsaitutor.practice;

import java.util.List;

public record PracticeAttemptResult(int score, int total, java.util.Map<String, String> answers,
        List<PracticeReview> review) {}
