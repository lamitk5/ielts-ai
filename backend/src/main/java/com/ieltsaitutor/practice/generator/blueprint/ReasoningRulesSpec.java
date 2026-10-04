package com.ieltsaitutor.practice.generator.blueprint;

public record ReasoningRulesSpec(
        boolean requireVerbatimEvidenceSpan,
        boolean disallowAmbiguousDistractors,
        int maxAnswerLengthWords) {

    public ReasoningRulesSpec {
        if (maxAnswerLengthWords <= 0) maxAnswerLengthWords = 3;
    }
}
