package com.ieltsaitutor.practice.generator.ai;

import java.util.List;

public record RawQuestionPayload(
        String id,
        String taskType,
        String prompt,
        List<String> options,
        String answerKey,
        String evidenceSpan,
        String explanation) {

    public RawQuestionPayload {
        if (options == null) options = List.of();
        if (evidenceSpan == null) evidenceSpan = "";
        if (explanation == null) explanation = "";
    }
}
