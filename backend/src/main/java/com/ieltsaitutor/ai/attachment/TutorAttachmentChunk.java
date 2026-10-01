package com.ieltsaitutor.ai.attachment;

import java.util.UUID;

public record TutorAttachmentChunk(UUID attachmentId, String filename, Integer pageNumber,
        String sectionLabel, int chunkIndex, String content, int tokenEstimate) {}
