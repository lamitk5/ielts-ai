package com.ieltsaitutor.tutor.memory;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository {
    void saveConversation(AiConversation conversation);
    Optional<AiConversation> findConversation(UUID userId, UUID id);
    void saveMessage(AiMessage message);
}
