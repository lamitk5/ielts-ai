package com.ieltsaitutor.assessment.objective;

public record QuestionScore(String questionId, String questionType, String learnerAnswer,
        String normalizedLearnerAnswer, String correctAnswer, boolean correct,
        String evidenceReference, String explanation) {}
