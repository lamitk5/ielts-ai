package com.ieltsaitutor.ai.attachment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TutorAttachmentCleanupService {
    private final TutorAttachmentRepository repository;
    private final TutorAttachmentStorage storage;

    @Autowired
    public TutorAttachmentCleanupService(TutorAttachmentRepository repository, TutorAttachmentStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public int cleanupRemovedOrExpired() {
        int cleaned = 0;
        for (TutorAttachment attachment : repository.findRemovable()) {
            try {
                storage.delete(attachment.storageKey());
                cleaned++;
            } catch (java.io.IOException ignored) {
                // Keep the row for a later bounded cleanup attempt.
            }
        }
        return cleaned;
    }
}
