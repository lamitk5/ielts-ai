package com.ieltsaitutor.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiChatResponse(
        String status,
        String answer,
        List<AiSource> sources,
        AiGrounding grounding,
        List<AiTutorReference> references,
        List<AiAttachmentSource> attachmentSources,
        Meta meta,
        Instant timestamp
) {
    public AiChatResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
        references = references == null ? List.of() : List.copyOf(references);
        attachmentSources = attachmentSources == null ? List.of() : List.copyOf(attachmentSources);
    }

    public AiChatResponse(String status, String answer, List<AiSource> sources, AiGrounding grounding, Meta meta, Instant timestamp) {
        this(status, answer, sources, grounding, List.of(), List.of(), meta, timestamp);
    }

    public AiChatResponse(String status, String answer, List<AiSource> sources, AiGrounding grounding,
            List<AiTutorReference> references, Meta meta, Instant timestamp) {
        this(status, answer, sources, grounding, references, List.of(), meta, timestamp);
    }

    public record Meta(String requestId, UUID conversationId) {
        public Meta(String requestId) {
            this(requestId, null);
        }
    }
}
