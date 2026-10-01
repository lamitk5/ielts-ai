package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

class TutorAttachmentProcessingServiceTest {
    private final TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);

    @Test
    void submitQueuesWorkWithoutRunningItInline() {
        UUID attachmentId = UUID.randomUUID();
        var queued = new java.util.concurrent.atomic.AtomicReference<Runnable>();
        TaskExecutor executor = queued::set;
        TutorAttachmentProcessingService service = new TutorAttachmentProcessingService(
                repository, null, null, null, executor);

        service.submit(attachmentId);

        assertThat(queued.get()).isNotNull();
        verify(repository, org.mockito.Mockito.never()).findById(any());
    }

    @Test
    void transitionsStoredToProcessingToReady() {
        TutorAttachment attachment = attachment(TutorAttachment.AttachmentStatus.STORED);
        when(repository.findById(attachment.id())).thenReturn(Optional.of(attachment));
        TutorAttachmentProcessingService service = new TutorAttachmentProcessingService(repository, ignored -> { });

        service.process(attachment.id());

        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.PROCESSING, null);
        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.READY, null);
    }

    @Test
    void parserFailureBecomesFailed() {
        TutorAttachment attachment = attachment(TutorAttachment.AttachmentStatus.STORED);
        when(repository.findById(attachment.id())).thenReturn(Optional.of(attachment));
        TutorAttachmentProcessingService service = new TutorAttachmentProcessingService(repository, ignored -> {
            throw new IllegalArgumentException("parser");
        });

        service.process(attachment.id());

        verify(repository).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.FAILED, "ATTACHMENT_PROCESSING_FAILED");
    }

    @Test
    void providerFailureDoesNotInvalidateReady() {
        TutorAttachment attachment = attachment(TutorAttachment.AttachmentStatus.READY);
        when(repository.findById(attachment.id())).thenReturn(Optional.of(attachment));
        TutorAttachmentProcessingService service = new TutorAttachmentProcessingService(repository, ignored -> {
            throw new IllegalStateException("provider");
        });

        service.process(attachment.id());

        verify(repository, org.mockito.Mockito.never()).updateStatus(attachment.id(), TutorAttachment.AttachmentStatus.FAILED, "ATTACHMENT_PROCESSING_FAILED");
    }

    private static TutorAttachment attachment(TutorAttachment.AttachmentStatus status) {
        Instant now = Instant.now();
        return new TutorAttachment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "notes.txt", "notes.txt", "text/plain",
                AttachmentKind.DOCUMENT, 4, "sha", "key", status, null, now, 0, now, now, null);
    }
}
