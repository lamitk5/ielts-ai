package com.ieltsaitutor.learning.analytics;

public record QuestionTypeAnalytics(String questionType, int correct, int total, double accuracyPercent) {}
