package com.ieltsaitutor.ai.attachment;

import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TutorAttachmentProcessingService {
    private final TutorAttachmentRepository repository;
    private final Consumer<TutorAttachment> processor;

    @Autowired
    public TutorAttachmentProcessingService(TutorAttachmentRepository repository) {
        this(repository, ignored -> { });
    }

    public TutorAttachmentProcessingService(TutorAttachmentRepository repository, Consumer<TutorAttachment> processor) {
        this.repository = repository;
        this.processor = processor;
    }

    public void process(UUID attachmentId) {
        TutorAttachment attachment = repository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        if (attachment.status() == TutorAttachment.AttachmentStatus.READY
                || attachment.status() == TutorAttachment.AttachmentStatus.REMOVED
                || attachment.status() == TutorAttachment.AttachmentStatus.EXPIRED) return;
        repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.PROCESSING, null);
        try {
            processor.accept(attachment);
            repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.READY, null);
        } catch (RuntimeException exception) {
            repository.updateStatus(attachmentId, TutorAttachment.AttachmentStatus.FAILED, "ATTACHMENT_PROCESSING_FAILED");
        }
    }
}
