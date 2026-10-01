package com.ieltsaitutor.tutor.memory;

import java.time.Instant;
import java.util.UUID;

public record AiConversation(UUID id, UUID userId, String skill, String practiceSetId, UUID attemptId,
        String questionId, String title, ConversationStatus status, Instant createdAt, Instant updatedAt) {}
