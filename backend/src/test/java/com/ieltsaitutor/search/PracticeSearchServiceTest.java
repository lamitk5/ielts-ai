package com.ieltsaitutor.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PracticeSearchServiceTest {
    private PracticeSearchService service;

    @BeforeEach
    void setUp() {
        service = new PracticeSearchService();
    }

    @Test
    void searchesSyntheticPracticeCatalogWithoutProviderCalls() {
        List<PracticeSearchResult> results = service.search("IELTS Writing Task 1");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.skill().equalsIgnoreCase("Writing")));

        List<PracticeSearchResult> readingResults = service.search("reading");
        assertFalse(readingResults.isEmpty());
        assertEquals("reading-foundation-01", readingResults.get(0).id());
    }

    @Test
    void rejectsBlankOrOversizedQueries() {
        assertThrows(IllegalArgumentException.class, () -> service.search(" "));
        assertThrows(IllegalArgumentException.class, () -> service.search("a".repeat(121)));
    }

    @Test
    void searchesWritingPromptsAndSpeakingTopics() {
        List<PracticeSearchResult> writing = service.search("Essay Task 2");
        assertFalse(writing.isEmpty());
        assertTrue(writing.stream().anyMatch(r -> "WRITING_PROMPT".equals(r.resultType())));

        List<PracticeSearchResult> speaking = service.search("Speaking Part 2");
        assertFalse(speaking.isEmpty());
        assertTrue(speaking.stream().anyMatch(r -> "SPEAKING_TOPIC".equals(r.resultType())));
    }

    @Test
    void filtersBySkillAndResultType() {
        List<PracticeSearchResult> filteredBySkill = service.search("Task", null, "Writing", null, 0, 10);
        assertFalse(filteredBySkill.isEmpty());
        assertTrue(filteredBySkill.stream().allMatch(r -> r.skill().equalsIgnoreCase("Writing")));

        List<PracticeSearchResult> filteredByType = service.search("Task", null, null, "WRITING_PROMPT", 0, 10);
        assertFalse(filteredByType.isEmpty());
        assertTrue(filteredByType.stream().allMatch(r -> r.resultType().equalsIgnoreCase("WRITING_PROMPT")));
    }

    @Test
    void supportsPaginationAndBounds() {
        List<PracticeSearchResult> page0 = service.search("a", null, null, null, 0, 2);
        List<PracticeSearchResult> page1 = service.search("a", null, null, null, 1, 2);

        assertNotNull(page0);
        assertNotNull(page1);
        if (!page0.isEmpty() && !page1.isEmpty()) {
            assertFalse(page0.get(0).id().equals(page1.get(0).id()));
        }
    }

    @Test
    void handlesVietnameseAndEnglishQueries() {
        List<PracticeSearchResult> vi = service.search("Bài luyện");
        assertFalse(vi.isEmpty());

        List<PracticeSearchResult> en = service.search("Reading");
        assertFalse(en.isEmpty());
    }
}
