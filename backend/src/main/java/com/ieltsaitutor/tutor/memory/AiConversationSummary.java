package com.ieltsaitutor.tutor.memory;

import java.time.Instant;
import java.util.UUID;

public record AiConversationSummary(UUID id, UUID conversationId, int revision, String summaryText,
        int coveredThroughSequence, Instant createdAt) {}
