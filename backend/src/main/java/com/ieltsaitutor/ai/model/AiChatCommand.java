package com.ieltsaitutor.ai.model;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.ChatHistoryItem;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.ieltsaitutor.ai.provider.ProviderCapability;

public record AiChatCommand(
        String message,
        AiChatContext context,
        List<ChatHistoryItem> history,
        String requestId,
        String groundedEvidence,
        List<AiAttachmentPart> attachments,
        Set<ProviderCapability> requiredCapabilities) {

    public AiChatCommand {
        history = history == null ? List.of() : List.copyOf(history);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
        requiredCapabilities = requiredCapabilities == null ? Set.of() : Set.copyOf(requiredCapabilities);
    }

    public AiChatCommand(String message, AiChatContext context, List<ChatHistoryItem> history, String requestId) {
        this(message, context, history, requestId, null, List.of(), Set.of());
    }

    public AiChatCommand(String message, AiChatContext context, List<ChatHistoryItem> history) {
        this(message, context, history, UUID.randomUUID().toString(), null, List.of(), Set.of());
    }

    public AiChatCommand(String message, AiChatContext context, List<ChatHistoryItem> history, String requestId,
            String groundedEvidence) {
        this(message, context, history, requestId, groundedEvidence, List.of(), Set.of());
    }
}
