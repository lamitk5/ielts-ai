package com.ieltsaitutor.ai.attachment;

import java.util.Arrays;
import java.util.UUID;

public record RenderedAttachmentPage(UUID attachmentId, int pageNumber, String mediaType, byte[] imageBytes) {
    public RenderedAttachmentPage {
        if (pageNumber < 1 || mediaType == null || mediaType.isBlank() || imageBytes == null
                || imageBytes.length == 0) throw new IllegalArgumentException("Rendered attachment page is invalid");
        imageBytes = Arrays.copyOf(imageBytes, imageBytes.length);
    }

    @Override
    public byte[] imageBytes() { return Arrays.copyOf(imageBytes, imageBytes.length); }
}
