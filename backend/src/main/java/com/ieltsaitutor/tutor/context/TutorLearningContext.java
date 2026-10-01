package com.ieltsaitutor.tutor.context;

import java.util.List;

/** Bounded, server-resolved facts that may be given to Tutor orchestration. */
public record TutorLearningContext(boolean available, String skill, String contextText,
        String questionId, String questionPrompt, String selectedAnswer, String correctAnswer, String explanation,
        Integer score, Integer total, String writingTaskId, String writingText,
        String speakingPromptId, String speakingTranscript, List<String> mistakes, String rawClientData) {
    public TutorLearningContext {
        contextText = contextText == null ? "" : limit(contextText, 2_000);
        writingText = limitNullable(writingText, 12_000);
        speakingTranscript = limitNullable(speakingTranscript, 12_000);
        mistakes = mistakes == null ? List.of() : List.copyOf(mistakes.stream().limit(20).toList());
        rawClientData = null;
    }

    public static TutorLearningContext absent(String skill) {
        return new TutorLearningContext(false, skill, "", null, null, null, null, null,
                null, null, null, null, null, null, List.of(), null);
    }

    private static String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    private static String limitNullable(String value, int max) { return value == null ? null : limit(value, max); }
}
