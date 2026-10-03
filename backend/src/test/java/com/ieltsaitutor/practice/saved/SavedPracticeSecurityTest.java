package com.ieltsaitutor.practice.saved;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;

class SavedPracticeSecurityTest {
    private SavedPracticeRepository repository;
    private ApprovedPracticeCatalogService catalogService;
    private SavedPracticeService service;

    @BeforeEach
    void setUp() {
        repository = mock(SavedPracticeRepository.class);
        catalogService = mock(ApprovedPracticeCatalogService.class);
        service = new SavedPracticeService(repository, catalogService);
    }

    @Test
    void savedPracticeIsStrictlyOwnerScoped() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        SavedPractice itemA = new SavedPractice(UUID.randomUUID(), userA, "reading-01", "reading", "Reading 1", Instant.now(), true);
        when(repository.findByUser(userA, null, 0, 20)).thenReturn(List.of(itemA));
        when(repository.findByUser(userB, null, 0, 20)).thenReturn(List.of());

        List<SavedPractice> listA = service.list(userA, null, 0, 20);
        List<SavedPractice> listB = service.list(userB, null, 0, 20);

        assertFalse(listA.isEmpty());
        assertTrue(listB.isEmpty());
        verify(repository).findByUser(eq(userA), any(), eq(0), eq(20));
        verify(repository).findByUser(eq(userB), any(), eq(0), eq(20));
    }

    @Test
    void userACannotUnsaveUserBPractice() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        String setId = "reading-foundation-01";

        service.unsave(userA, setId);

        // Verify repository deletion was called only for userA, never userB
        verify(repository).delete(eq(userA), eq(setId));
        verify(repository, never()).delete(eq(userB), eq(setId));
    }
}
