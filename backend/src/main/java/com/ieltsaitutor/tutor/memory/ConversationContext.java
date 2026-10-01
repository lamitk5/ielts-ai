package com.ieltsaitutor.tutor.memory;

import java.util.List;

public record ConversationContext(AiConversationSummary summary, List<AiMessage> messages) {
    public ConversationContext { messages = messages == null ? List.of() : List.copyOf(messages); }
}
