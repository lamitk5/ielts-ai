package com.ieltsaitutor.ai.dto;

import java.util.UUID;

import com.ieltsaitutor.ai.attachment.AttachmentKind;
import com.ieltsaitutor.ai.attachment.TutorAttachment;

public record AiAttachmentSource(UUID attachmentId, String filename, AttachmentKind kind, long sizeBytes,
        TutorAttachment.AttachmentStatus status) {}
