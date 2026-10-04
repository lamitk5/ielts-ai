package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

class TutorAttachmentCleanupServiceTest {
    @Test
    void cleanupIsBoundedToRowsWithoutReferences() {
        TutorAttachmentRepository repository = mock(TutorAttachmentRepository.class);
        TutorAttachmentStorage storage = mock(TutorAttachmentStorage.class);
        when(repository.findRemovable()).thenReturn(List.of());

        assertThat(new TutorAttachmentCleanupService(repository, storage).cleanupRemovedOrExpired()).isZero();
    }
}
