package com.ieltsaitutor.ai.attachment;

public record TutorAttachmentValidationResult(
        String canonicalFilename,
        String contentType,
        AttachmentKind kind,
        long sizeBytes,
        byte[] content,
        String errorCode,
        String errorMessage
) {
    public boolean valid() {
        return errorCode == null;
    }
}
