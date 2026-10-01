package com.ieltsaitutor.learning.intelligence;

public record MistakeEvidence(Skill skill, String questionType, String learnerAnswer, String correctAnswer,
        boolean correct, String questionId) {}
