package com.ieltsaitutor.ai.model;

import com.ieltsaitutor.ai.dto.AiChatContext;
import com.ieltsaitutor.ai.dto.ChatHistoryItem;

import java.util.List;

public record AiChatCommand(String message, AiChatContext context, List<ChatHistoryItem> history) {
}
