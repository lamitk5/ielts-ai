package com.ieltsaitutor.ai.attachment;

import java.util.List;

import com.ieltsaitutor.ai.dto.AiAttachmentSource;
import com.ieltsaitutor.ai.model.AiAttachmentPart;

public record TutorAttachmentChatContext(AttachmentChatScope scope, List<AiAttachmentPart> parts,
        List<AiAttachmentSource> sources) {
    public TutorAttachmentChatContext {
        parts = parts == null ? List.of() : List.copyOf(parts);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

}
