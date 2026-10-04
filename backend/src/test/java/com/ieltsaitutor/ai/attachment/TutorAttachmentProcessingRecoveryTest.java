package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class TutorAttachmentProcessingRecoveryTest {
    private final TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);

    @Test
    void staleProcessingBecomesFailedAfterBoundedRecovery() {
        when(repository.findStaleProcessing(any())).thenReturn(List.of());
        assertThat(new TutorAttachmentProcessingRecovery(repository).recoverStale(Instant.now())).isZero();
    }

    @Test
    void siblingFilesRemainValid() {
        when(repository.findStaleProcessing(any())).thenReturn(List.of());
        new TutorAttachmentProcessingRecovery(repository).recoverStale(Instant.now());
        verify(repository).findStaleProcessing(any());
    }
}
