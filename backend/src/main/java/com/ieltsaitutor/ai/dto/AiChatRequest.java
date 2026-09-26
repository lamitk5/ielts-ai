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
        UUID conversationId
) {
    public AiChatRequest(String message, AiChatContext context, List<ChatHistoryItem> history) {
        this(message, context, history, null);
    }

    public AiChatRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
