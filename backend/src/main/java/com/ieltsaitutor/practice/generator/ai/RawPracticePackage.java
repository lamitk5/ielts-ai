package com.ieltsaitutor.practice.generator.ai;

import java.time.Instant;
import java.util.List;

public record RawPracticePackage(
        String title,
        RawPassagePayload passage,
        List<RawQuestionPayload> questions,
        String modelId,
        String promptTemplateVersion,
        Instant generatedAt) {

    public RawPracticePackage {
        if (title == null || title.isBlank()) title = "IELTS Academic Reading Practice";
        if (passage == null) passage = new RawPassagePayload(title, List.of());
        if (questions == null) questions = List.of();
        if (modelId == null) modelId = "unknown";
        if (promptTemplateVersion == null) promptTemplateVersion = "v1.0";
        if (generatedAt == null) generatedAt = Instant.now();
    }
}
