package com.ieltsaitutor.ai.attachment;

import java.util.List;
import java.util.UUID;

public record TutorAttachmentBatchResponse(List<AttachmentView> attachments) {
    public record AttachmentView(UUID id, UUID conversationId, String filename, String contentType,
            AttachmentKind kind, long sizeBytes, TutorAttachment.AttachmentStatus status, String errorCode) {
        public static AttachmentView from(TutorAttachment attachment) {
            return new AttachmentView(attachment.id(), attachment.conversationId(), attachment.sanitizedFilename(),
                    attachment.contentType(), attachment.kind(), attachment.sizeBytes(), attachment.status(),
                    attachment.processingErrorCode());
        }
    }
}
