package com.ieltsaitutor.ai.attachment;

import java.util.List;

public record VisionContextResult(Status status, String answer, List<RenderedAttachmentPage> pages,
        int remainingPageCount, String warning, String errorCode) {
    public VisionContextResult {
        answer = answer == null ? "" : answer;
        pages = pages == null ? List.of() : List.copyOf(pages);
    }

    public enum Status { TEXT_READY, ANSWERED, ERROR }
}
