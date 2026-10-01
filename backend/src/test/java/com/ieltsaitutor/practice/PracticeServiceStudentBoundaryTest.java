package com.ieltsaitutor.practice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.repository.DatabasePracticeCatalogStore;

class PracticeServiceStudentBoundaryTest {

    private DatabasePracticeCatalogStore dbStore;
    private SyntheticPracticeCatalog catalog;
    private PracticeAttemptStore attemptStore;
    private PracticeService service;

    @BeforeEach
    void setUp() {
        dbStore = new DatabasePracticeCatalogStore();
        catalog = new SyntheticPracticeCatalog(dbStore);
        attemptStore = mock(PracticeAttemptStore.class);
        service = new PracticeService(catalog, attemptStore);
    }

    @Test
    void unapprovedDraftsAreExcludedFromStudentQueries() {
        // Only approved sets are placed into dbStore during hydration
        List<PracticeSet> readingSets = service.sets("reading");
        assertEquals(1, readingSets.size());
        assertEquals("reading-foundation-01", readingSets.get(0).id());

        // Attempting to query an unapproved draft ID throws exception
        assertThrows(IllegalArgumentException.class, () -> service.set("reading", "draft-set-uuid-1234"));
    }

    @Test
    void approvedHydratedSetIsAvailableForStudentPracticeAndAttempt() {
        PracticeSet approvedSet = new PracticeSet(
                "reading-synth-approved1",
                "reading",
                "Approved AI Reading Passage",
                "Curated set",
                List.of(new PracticeQuestion("q1", "What is true?", List.of("A", "B"), "A", "Explanation")),
                new PracticePassage("Passage Title", List.of(new PracticeParagraph("p1", "Text")))
        );
        dbStore.save(approvedSet);

        List<PracticeSet> readingSets = service.sets("reading");
        assertEquals(2, readingSets.size());
        assertTrue(readingSets.stream().anyMatch(s -> s.id().equals("reading-synth-approved1")));

        PracticeSet retrieved = service.set("reading", "reading-synth-approved1");
        assertNotNull(retrieved);
        assertEquals("Approved AI Reading Passage", retrieved.title());

        UUID attemptId = UUID.randomUUID();
        when(attemptStore.saveAndReturn(any(), any(), any(), anyInt(), anyInt(), any())).thenReturn(attemptId);

        PracticeAttemptResult result = service.submit("reading", "reading-synth-approved1", Map.of("q1", "A"), UUID.randomUUID());
        assertNotNull(result);
        assertEquals(1, result.score());
        assertEquals(1, result.total());
    }
}
