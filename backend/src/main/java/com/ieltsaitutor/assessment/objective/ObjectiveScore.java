package com.ieltsaitutor.assessment.objective;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record ObjectiveScore(int correctCount, int totalQuestions, BigDecimal accuracy,
        String scoringPolicyVersion, List<QuestionScore> questionResults) {
    public ObjectiveScore {
        if (correctCount < 0 || totalQuestions < 0 || correctCount > totalQuestions)
            throw new IllegalArgumentException("Invalid objective score");
        questionResults = questionResults == null ? List.of() : List.copyOf(questionResults);
        accuracy = totalQuestions == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(correctCount * 100.0 / totalQuestions).setScale(2, RoundingMode.HALF_UP);
    }
}
