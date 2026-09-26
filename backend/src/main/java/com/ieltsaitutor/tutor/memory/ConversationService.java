package com.ieltsaitutor.tutor.memory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class ConversationService {
    private final ConversationRepository repository;
    public ConversationService(ConversationRepository repository) { this.repository = repository; }

    public AiConversation create(UUID userId, String skill, String practiceSetId, UUID attemptId, String questionId, String title) {
        Instant now = Instant.now();
        AiConversation result = new AiConversation(UUID.randomUUID(), userId, skill, practiceSetId, attemptId, questionId,
                title == null || title.isBlank() ? "Tutor conversation" : title.trim(), ConversationStatus.ACTIVE, now, now);
        if (userId != null) repository.saveConversation(result);
        return result;
    }

    public Optional<AiConversation> findOwned(UUID userId, UUID id) {
        return userId == null || id == null ? Optional.empty() : repository.findConversation(userId, id);
    }

    public void appendMessage(UUID userId, UUID conversationId, AiMessage message) {
        if (userId != null && findOwned(userId, conversationId).isPresent()) repository.saveMessage(message);
    }

    public java.util.List<AiConversation> listOwned(UUID userId) { return userId == null ? java.util.List.of() : repository.findConversations(userId); }
    public java.util.List<AiMessage> messages(UUID userId, UUID conversationId) { return findOwned(userId, conversationId).isPresent() ? repository.findMessages(userId, conversationId) : java.util.List.of(); }
    public boolean archive(UUID userId, UUID conversationId) { return userId != null && repository.archive(userId, conversationId); }
}
