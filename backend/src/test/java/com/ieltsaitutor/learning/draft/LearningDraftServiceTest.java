package com.ieltsaitutor.learning.draft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LearningDraftServiceTest {
    private LearningDraftRepository repository;
    private LearningDraftService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(LearningDraftRepository.class);
        service = new LearningDraftService(repository);
    }

    @Test
    void savesInitialDraftWhenNoneExists() {
        when(repository.findActive(userId, "WRITING", "task-1")).thenReturn(Optional.empty());
        when(repository.save(any(LearningDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningDraft saved = service.saveDraft(userId, "WRITING", "task-1", "Initial draft paragraph.", null);

        assertNotNull(saved.id());
        assertEquals(userId, saved.userId());
        assertEquals("WRITING", saved.skill());
        assertEquals("task-1", saved.referenceId());
        assertEquals("Initial draft paragraph.", saved.contentSnapshot());
        assertEquals(1L, saved.version());
        assertEquals(LearningDraft.DraftStatus.ACTIVE, saved.status());
    }

    @Test
    void incrementsVersionOnUpdate() {
        UUID draftId = UUID.randomUUID();
        LearningDraft existing = new LearningDraft(
                draftId, userId, "WRITING", "task-1",
                "Old content", 1L, LearningDraft.DraftStatus.ACTIVE,
                Instant.now(), Instant.now(), null
        );

        when(repository.findActive(userId, "WRITING", "task-1")).thenReturn(Optional.of(existing));
        when(repository.save(any(LearningDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningDraft updated = service.saveDraft(userId, "WRITING", "task-1", "Updated content", 1L);

        assertEquals(draftId, updated.id());
        assertEquals(2L, updated.version());
        assertEquals("Updated content", updated.contentSnapshot());
    }

    @Test
    void throwsConflictExceptionWhenVersionMismatch() {
        UUID draftId = UUID.randomUUID();
        LearningDraft existing = new LearningDraft(
                draftId, userId, "WRITING", "task-1",
                "Server newer content", 3L, LearningDraft.DraftStatus.ACTIVE,
                Instant.now(), Instant.now(), null
        );

        when(repository.findActive(userId, "WRITING", "task-1")).thenReturn(Optional.of(existing));

        assertThrows(DraftConflictException.class, () ->
                service.saveDraft(userId, "WRITING", "task-1", "Client stale text", 1L)
        );
    }

    @Test
    void rejectsOversizedContent() {
        String oversized = "a".repeat(50_001);
        assertThrows(IllegalArgumentException.class, () ->
                service.saveDraft(userId, "WRITING", "task-1", oversized, null)
        );
    }
}
