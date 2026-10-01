package com.ieltsaitutor.tutor.memory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ieltsaitutor.ai.dto.AiSource;
import com.ieltsaitutor.ai.attachment.TutorAttachmentHistoryView;

public record AiMessage(UUID id, UUID conversationId, int sequenceNo, AiMessageRole role, String content,
        String responseStatus, String groundingStatus, List<AiSource> citations, Map<String, Object> contextSnapshot,
        Instant createdAt, List<TutorAttachmentHistoryView> attachments) {
    public AiMessage {
        citations = citations == null ? List.of() : List.copyOf(citations);
        contextSnapshot = contextSnapshot == null ? Map.of() : Map.copyOf(contextSnapshot);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public AiMessage(UUID id, UUID conversationId, int sequenceNo, AiMessageRole role, String content,
            String responseStatus, String groundingStatus, List<AiSource> citations, Map<String, Object> contextSnapshot,
            Instant createdAt) {
        this(id, conversationId, sequenceNo, role, content, responseStatus, groundingStatus, citations,
                contextSnapshot, createdAt, List.of());
    }
}
