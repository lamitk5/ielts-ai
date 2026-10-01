package com.ieltsaitutor.ai.attachment;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TutorAttachmentProcessingRecovery {
    private final TutorAttachmentRepository repository;
    private final TutorAttachmentProcessingProperties properties;

    @Autowired
    public TutorAttachmentProcessingRecovery(TutorAttachmentRepository repository,
            TutorAttachmentProcessingProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public TutorAttachmentProcessingRecovery(TutorAttachmentRepository repository) {
        this(repository, new TutorAttachmentProcessingProperties(java.time.Duration.ofMinutes(5), 1));
    }

    public int recoverStale(Instant now) {
        Instant cutoff = now.minus(properties.staleTimeout());
        int recovered = 0;
        for (TutorAttachment attachment : repository.findStaleProcessing(cutoff)) {
            if (attachment.processingAttempts() >= properties.maxRecoveryAttempts()) {
                repository.updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.FAILED, "ATTACHMENT_PROCESSING_TIMEOUT");
                recovered++;
            }
        }
        return recovered;
    }
}
