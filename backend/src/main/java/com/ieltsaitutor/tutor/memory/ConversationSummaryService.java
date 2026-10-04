package com.ieltsaitutor.tutor.memory;

import java.time.Instant;
import java.util.UUID;

public class ConversationSummaryService {
    private final ConversationRepository repository;
    public ConversationSummaryService(ConversationRepository repository) { this.repository = repository; }

    public void save(UUID userId, UUID conversationId, String text, int coveredThroughSequence) {
        if (userId == null || repository.findConversation(userId, conversationId).isEmpty()) return;
        String bounded = text == null ? "" : text.substring(0, Math.min(4000, text.length()));
        repository.saveSummary(new AiConversationSummary(UUID.randomUUID(), conversationId, 1, bounded,
                Math.max(0, coveredThroughSequence), Instant.now()));
    }
}
