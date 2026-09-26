package com.ieltsaitutor.tutor.memory;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {
    void saveConversation(AiConversation conversation);
    Optional<AiConversation> findConversation(UUID userId, UUID id);
    void saveMessage(AiMessage message);
    default java.util.List<AiConversation> findConversations(UUID userId) { return java.util.List.of(); }
    default java.util.List<AiMessage> findMessages(UUID userId, UUID id) { return java.util.List.of(); }
    default boolean archive(UUID userId, UUID id) { return false; }
    default void saveSummary(AiConversationSummary summary) {}
}
