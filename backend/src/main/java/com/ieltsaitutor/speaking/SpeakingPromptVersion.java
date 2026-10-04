package com.ieltsaitutor.speaking;

import java.util.Objects;

public record SpeakingPromptVersion(
        String promptId,
        String version,
        String topic,
        String part,
        int defaultPreparationSeconds,
        int defaultResponseSeconds) {

    public SpeakingPromptVersion {
        Objects.requireNonNull(promptId, "promptId must not be null");
        version = version != null && !version.isBlank() ? version.trim() : "v1";
        topic = Objects.requireNonNullElse(topic, "General Speaking");
        part = Objects.requireNonNullElse(part, "PART_1");
        if (defaultPreparationSeconds < 0) throw new IllegalArgumentException("defaultPreparationSeconds cannot be negative");
        if (defaultResponseSeconds < 0) throw new IllegalArgumentException("defaultResponseSeconds cannot be negative");
    }
}
