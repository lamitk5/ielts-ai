package com.ieltsaitutor.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AiChatRequest(
        @NotBlank(message = "message is required") @Size(max = 4000, message = "message is too long") String message,
        @Valid AiChatContext context,
        @Size(max = 8, message = "history is too long") List<@Valid ChatHistoryItem> history,
        UUID conversationId,
        @Size(max = 5, message = "attachments are limited to five files") List<UUID> attachmentIds
) {
    public AiChatRequest(String message, AiChatContext context, List<ChatHistoryItem> history) {
        this(message, context, history, null, List.of());
    }

    public AiChatRequest(String message, AiChatContext context, List<ChatHistoryItem> history, UUID conversationId) {
        this(message, context, history, conversationId, List.of());
    }

    public AiChatRequest {
        history = history == null ? List.of() : List.copyOf(history);
        attachmentIds = attachmentIds == null ? List.of() : List.copyOf(attachmentIds);
    }
}
