package com.ieltsaitutor.tutor.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiSource;

class ConversationContextAssemblerTest {
    @Test
    void contextUsesOnlyTheEightMostRecentMessages() {
        UUID user = UUID.randomUUID();
        UUID conversation = UUID.randomUUID();
        List<AiMessage> messages = new ArrayList<>();
        for (int i = 1; i <= 12; i++) messages.add(new AiMessage(UUID.randomUUID(), conversation, i,
                AiMessageRole.USER, "message-" + i, "ANSWERED", "NOT_APPLICABLE", List.of(new AiSource("s", "Source", "1")), java.util.Map.of(), Instant.now()));
        ConversationRepository repository = new ConversationRepository() {
            @Override public void saveConversation(AiConversation c) {}
            @Override public Optional<AiConversation> findConversation(UUID u, UUID id) { return Optional.empty(); }
            @Override public void saveMessage(AiMessage m) {}
            @Override public List<AiMessage> findMessages(UUID u, UUID id) { return messages; }
        };

        ConversationContext context = new ConversationContextAssembler(repository).assemble(user, conversation);

        assertEquals(8, context.messages().size());
        assertEquals("message-5", context.messages().get(0).content());
        assertEquals("message-12", context.messages().get(7).content());
    }
}
