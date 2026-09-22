package com.ieltsaitutor.ai.dto;

import java.time.Instant;
import java.util.List;

public record AiChatResponse(
        String status,
        String answer,
        List<AiSource> sources,
        AiGrounding grounding,
        Meta meta,
        Instant timestamp
) {
    public record Meta(String requestId) {
    }
}
