package com.ieltsaitutor.tutor.memory;

import java.util.List;

public class ConversationRetentionService {
    public List<AiMessage> visibleMessages(List<AiMessage> messages) {
        if (messages == null) return List.of();
        int start = Math.max(0, messages.size() - 200);
        return List.copyOf(messages.subList(start, messages.size()));
    }
}
