package com.ieltsaitutor.practice;

import java.util.List;

public record PracticeQuestion(String id, String prompt, List<String> options, String answerKey, String explanation,
        String questionType, String evidenceReference) {
    public PracticeQuestion(String id, String prompt, List<String> options, String answerKey, String explanation) {
        this(id, prompt, options, answerKey, explanation, "MULTIPLE_CHOICE", "");
    }

    public PracticeQuestion {
        options = options == null ? List.of() : List.copyOf(options);
        questionType = questionType == null || questionType.isBlank() ? "MULTIPLE_CHOICE" : questionType.trim();
        evidenceReference = evidenceReference == null ? "" : evidenceReference.trim();
    }
}
