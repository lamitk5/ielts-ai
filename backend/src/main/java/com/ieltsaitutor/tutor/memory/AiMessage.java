package com.ieltsaitutor.tutor.memory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ieltsaitutor.ai.dto.AiSource;

public record AiMessage(UUID id, UUID conversationId, int sequenceNo, AiMessageRole role, String content,
        String responseStatus, String groundingStatus, List<AiSource> citations, Map<String, Object> contextSnapshot,
        Instant createdAt) {
    public AiMessage { citations = citations == null ? List.of() : List.copyOf(citations); contextSnapshot = contextSnapshot == null ? Map.of() : Map.copyOf(contextSnapshot); }
}
