package com.ieltsaitutor.ai.model;

import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;

import com.ieltsaitutor.ai.attachment.AttachmentKind;

public record AiAttachmentPart(UUID attachmentId, String filename, String mediaType, AttachmentKind kind,
        Supplier<InputStream> openStream) {
    public AiAttachmentPart {
        if (attachmentId == null || filename == null || filename.isBlank() || mediaType == null || mediaType.isBlank()
                || kind == null || openStream == null) {
            throw new IllegalArgumentException("AI attachment part is invalid");
        }
    }
}
