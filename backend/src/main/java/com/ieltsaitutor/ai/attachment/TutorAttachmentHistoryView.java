package com.ieltsaitutor.ai.attachment;

import java.util.UUID;

public record TutorAttachmentHistoryView(UUID id, String filename, AttachmentKind kind, long sizeBytes,
        TutorAttachment.AttachmentStatus status, String preview) {
    public TutorAttachmentHistoryView {
        preview = preview == null ? filename : preview;
    }
}
