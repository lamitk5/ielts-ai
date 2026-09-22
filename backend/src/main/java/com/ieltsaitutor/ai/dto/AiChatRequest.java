package com.ieltsaitutor.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiChatRequest(
        @NotBlank(message = "message is required") @Size(max = 4000, message = "message is too long") String message,
        @Valid AiChatContext context,
        @Size(max = 8, message = "history is too long") List<@Valid ChatHistoryItem> history
) {
    public AiChatRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
