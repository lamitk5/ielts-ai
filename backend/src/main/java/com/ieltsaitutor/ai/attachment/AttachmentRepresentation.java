package com.ieltsaitutor.ai.attachment;

import java.util.List;
import java.util.UUID;

public record AttachmentRepresentation(UUID attachmentId, String filename, List<String> sections,
        int tokenEstimate) {
    public AttachmentRepresentation {
        if (attachmentId == null || filename == null || filename.isBlank() || sections == null || tokenEstimate < 0) {
            throw new IllegalArgumentException("Attachment representation is invalid");
        }
        sections = List.copyOf(sections);
    }

    public String text() { return String.join("\n\n", sections); }
}
