package com.ieltsaitutor.tutor.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConversationSummaryServiceTest {
    @Test
    void summaryIsBoundedToOneThousandTokenEquivalentAndOwned() {
        UUID user = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        List<AiConversationSummary> saved = new ArrayList<>();
        ConversationRepository repository = new ConversationRepository() {
            @Override public void saveConversation(AiConversation c) {}
            @Override public Optional<AiConversation> findConversation(UUID u, UUID i) { return Optional.of(new AiConversation(id, user, "reading", null, null, null, "x", ConversationStatus.ACTIVE, java.time.Instant.now(), java.time.Instant.now())); }
            @Override public void saveMessage(AiMessage m) {}
            @Override public void saveSummary(AiConversationSummary summary) { saved.add(summary); }
        };
        String raw = "x".repeat(10000);

        new ConversationSummaryService(repository).save(user, id, raw, 12);

        assertEquals(1, saved.size());
        assertEquals(4000, saved.get(0).summaryText().length());
    }
}
