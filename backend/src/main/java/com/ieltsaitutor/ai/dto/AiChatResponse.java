package com.ieltsaitutor.ai.dto;

import java.time.Instant;
import java.util.List;

public record AiChatResponse(
        String status,
        String answer,
        List<AiSource> sources,
        AiGrounding grounding,
        List<AiTutorReference> references,
        Meta meta,
        Instant timestamp
) {
    public AiChatResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
        references = references == null ? List.of() : List.copyOf(references);
    }

    public AiChatResponse(String status, String answer, List<AiSource> sources, AiGrounding grounding, Meta meta, Instant timestamp) {
        this(status, answer, sources, grounding, List.of(), meta, timestamp);
    }

    public record Meta(String requestId) {
    }
}
