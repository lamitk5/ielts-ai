package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TutorAttachmentServiceTest {
    @Test
    void batchUploadRunsLifecycleProcessingBeforeReturningMetadata() throws Exception {
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        TutorAttachmentValidator validator = mock(TutorAttachmentValidator.class);
        TutorAttachmentProcessingService processing = mock(TutorAttachmentProcessingService.class);
        byte[] content = "guide".getBytes();
        when(validator.validate(any())).thenReturn(new TutorAttachmentValidationResult(
                "guide.txt", "text/plain", AttachmentKind.DOCUMENT, content.length, content, null, null));
        doNothing().when(storage).store(any(), any(), any(Long.class));
        TutorAttachment ready = new TutorAttachment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "guide.txt", "guide.txt", "text/plain", AttachmentKind.DOCUMENT, content.length, "sha", "key",
                TutorAttachment.AttachmentStatus.READY, null, null, 0, Instant.now(), Instant.now(), null);
        when(repository.findById(any())).thenReturn(Optional.of(ready));

        TutorAttachmentService service = new TutorAttachmentService(repository, storage, validator, processing);

        List<TutorAttachment> result = service.uploadBatch(UUID.randomUUID(), UUID.randomUUID(), List.of(
                new MockMultipartFile("files", "guide.txt", "text/plain", content)));

        verify(processing).process(any());
        assertThat(result).singleElement().extracting(TutorAttachment::status)
                .isEqualTo(TutorAttachment.AttachmentStatus.READY);
    }
}
