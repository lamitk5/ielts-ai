package com.ieltsaitutor.ai.attachment;

import java.util.List;
import java.util.UUID;

public record AttachmentChatScope(UUID userId, UUID conversationId, List<UUID> attachmentIds,
        TutorAttachmentQuestionMode mode) {
    public AttachmentChatScope {
        if (userId == null || conversationId == null || attachmentIds == null || attachmentIds.isEmpty() || mode == null) {
            throw new IllegalArgumentException("Attachment chat scope is required");
        }
        attachmentIds = List.copyOf(attachmentIds);
    }
}
