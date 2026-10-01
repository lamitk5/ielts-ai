package com.ieltsaitutor.tutor.memory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ConversationContextAssembler {
    private final ConversationRepository repository;
    public ConversationContextAssembler(ConversationRepository repository) { this.repository = repository; }

    public ConversationContext assemble(UUID userId, UUID conversationId) {
        List<AiMessage> messages = repository.findMessages(userId, conversationId).stream()
                .sorted(Comparator.comparingInt(AiMessage::sequenceNo)).toList();
        int start = Math.max(0, messages.size() - 8);
        return new ConversationContext(null, messages.subList(start, messages.size()));
    }
}
