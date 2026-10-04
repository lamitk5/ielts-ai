package com.ieltsaitutor.practice.saved;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;
import com.ieltsaitutor.practice.catalog.PracticePublication;

class SavedPracticeServiceTest {
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
    void savesApprovedPublication() {
        UUID userId = UUID.randomUUID();
        String setId = "reading-academic-01";
        PracticePublication pub = new PracticePublication(setId, UUID.randomUUID(), UUID.randomUUID(), "reading", true, 1, "test", Instant.now());
        when(catalogService.findActive(setId)).thenReturn(Optional.of(pub));
        when(repository.save(any(SavedPractice.class))).thenAnswer(inv -> inv.getArgument(0));

        SavedPractice saved = service.save(userId, setId);

        assertNotNull(saved);
        assertEquals(userId, saved.userId());
        assertEquals(setId, saved.publishedSetId());
        assertEquals("reading", saved.skill());
        assertTrue(saved.available());
        verify(repository).save(any(SavedPractice.class));
    }

    @Test
    void savesApprovedSyntheticSet() {
        UUID userId = UUID.randomUUID();
        String setId = "reading-foundation-01";
        when(catalogService.findActive(setId)).thenReturn(Optional.empty());
        when(repository.save(any(SavedPractice.class))).thenAnswer(inv -> inv.getArgument(0));

        SavedPractice saved = service.save(userId, setId);

        assertNotNull(saved);
        assertEquals(userId, saved.userId());
        assertEquals(setId, saved.publishedSetId());
        assertEquals("reading", saved.skill());
    }

    @Test
    void rejectsUnapprovedOrNonExistentSet() {
        UUID userId = UUID.randomUUID();
        String setId = "unapproved-set-99";
        when(catalogService.findActive(setId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.save(userId, setId));
    }

    @Test
    void unsavesIdempotently() {
        UUID userId = UUID.randomUUID();
        String setId = "reading-foundation-01";

        service.unsave(userId, setId);
        verify(repository).delete(userId, setId);

        // Repeated unsave does not throw
        service.unsave(userId, setId);
        verify(repository, org.mockito.Mockito.times(2)).delete(userId, setId);
    }

    @Test
    void listsUserSavedPractices() {
        UUID userId = UUID.randomUUID();
        SavedPractice item = new SavedPractice(UUID.randomUUID(), userId, "reading-foundation-01", "reading", "Reading foundation", Instant.now(), true);
        when(repository.findByUser(userId, null, 0, 20)).thenReturn(List.of(item));

        List<SavedPractice> list = service.list(userId, null, 0, 20);

        assertEquals(1, list.size());
        assertEquals("reading-foundation-01", list.get(0).publishedSetId());
    }

    @Test
    void checksIfSaved() {
        UUID userId = UUID.randomUUID();
        when(repository.isSaved(userId, "reading-01")).thenReturn(true);
        when(repository.isSaved(userId, "reading-02")).thenReturn(false);

        assertTrue(service.isSaved(userId, "reading-01"));
        assertFalse(service.isSaved(userId, "reading-02"));
    }
}
