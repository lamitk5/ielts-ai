package com.ieltsaitutor.ai.model;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.ChatHistoryItem;

import java.util.List;
import java.util.UUID;

public record AiChatCommand(
        String message,
        AiChatContext context,
        List<ChatHistoryItem> history,
        String requestId,
        String groundedEvidence) {

    public AiChatCommand(String message, AiChatContext context, List<ChatHistoryItem> history, String requestId) {
        this(message, context, history, requestId, null);
    }

    public AiChatCommand(String message, AiChatContext context, List<ChatHistoryItem> history) {
        this(message, context, history, UUID.randomUUID().toString(), null);
    }
}
