package com.ieltsaitutor.ai.attachment;

import java.util.List;

public record AttachmentContext(TutorAttachmentQuestionMode mode, String evidence,
        List<RetrievedAttachmentChunk> sources, List<AttachmentRepresentation> representations, int tokenEstimate) {
    public AttachmentContext {
        evidence = evidence == null ? "" : evidence;
        sources = sources == null ? List.of() : List.copyOf(sources);
        representations = representations == null ? List.of() : List.copyOf(representations);
    }
}
