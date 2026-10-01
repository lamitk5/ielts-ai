package com.ieltsaitutor.ai.attachment;

public record TutorAttachmentContextBudget(int maxAttachmentTokens) {
    public TutorAttachmentContextBudget {
        if (maxAttachmentTokens <= 0) throw new IllegalArgumentException("Attachment context budget must be positive");
    }

    public static TutorAttachmentContextBudget defaults() { return new TutorAttachmentContextBudget(12_000); }
}
