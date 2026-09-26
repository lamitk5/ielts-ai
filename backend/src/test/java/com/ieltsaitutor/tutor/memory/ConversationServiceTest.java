package com.ieltsaitutor.tutor.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConversationServiceTest {
    @Test
    void authenticatedConversationIsPersistentAndUserScoped() {
        InMemoryConversations repository = new InMemoryConversations();
        ConversationService service = new ConversationService(repository);
        UUID userId = UUID.randomUUID();

        AiConversation conversation = service.create(userId, "reading", "set-1", null, null, "Reading help");

        assertTrue(repository.conversations.contains(conversation));
        assertTrue(service.findOwned(userId, conversation.id()).isPresent());
        assertTrue(service.findOwned(UUID.randomUUID(), conversation.id()).isEmpty());
    }

    @Test
    void guestConversationIsEphemeralAndCannotPersistMessages() {
        InMemoryConversations repository = new InMemoryConversations();
        ConversationService service = new ConversationService(repository);

        AiConversation conversation = service.create(null, "general", null, null, null, null);

        assertEquals(0, repository.conversations.size());
        service.appendMessage(null, conversation.id(), new AiMessage(UUID.randomUUID(), conversation.id(), 1,
                AiMessageRole.USER, "hello", "ANSWERED", "NOT_APPLICABLE", List.of(), java.util.Map.of(), Instant.now()));
        assertEquals(0, repository.messages.size());
    }

    static final class InMemoryConversations implements ConversationRepository {
        final List<AiConversation> conversations = new ArrayList<>();
        final List<AiMessage> messages = new ArrayList<>();
        @Override public void saveConversation(AiConversation conversation) { conversations.add(conversation); }
        @Override public Optional<AiConversation> findConversation(UUID userId, UUID id) { return conversations.stream().filter(c -> c.userId().equals(userId) && c.id().equals(id)).findFirst(); }
        @Override public void saveMessage(AiMessage message) { messages.add(message); }
    }
}
