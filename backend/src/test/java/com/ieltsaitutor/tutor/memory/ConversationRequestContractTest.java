package com.ieltsaitutor.tutor.memory;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiChatRequest;

class ConversationRequestContractTest {
    @Test
    void conversationIdIsAdditiveAndNullable() {
        assertNull(new AiChatRequest("hello", null, null).conversationId());
        UUID id = UUID.randomUUID();
        assertEquals(id, new AiChatRequest("hello", null, null, id).conversationId());
    }

    private static void assertEquals(Object expected, Object actual) { org.junit.jupiter.api.Assertions.assertEquals(expected, actual); }
}
