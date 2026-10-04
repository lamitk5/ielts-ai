package com.ieltsaitutor.assessment.objective;

public record QuestionResultView(String questionId, String questionType, String learnerAnswer,
        String correctAnswer, boolean correct, String evidenceReference, String explanation) {
    public static QuestionResultView from(QuestionResult item) {
        return new QuestionResultView(item.questionId(), item.questionType(), item.learnerAnswer(), item.correctAnswer(),
                item.correct(), item.evidenceReference(), item.explanation());
    }
}
