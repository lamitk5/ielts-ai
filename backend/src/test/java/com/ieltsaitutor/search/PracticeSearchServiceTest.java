package com.ieltsaitutor.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PracticeSearchServiceTest {
    @Test
    void searchesSyntheticPracticeCatalogWithoutProviderCalls() {
        PracticeSearchService service = new PracticeSearchService();

        assertEquals("Writing", service.search("IELTS Writing Task 1").get(0).skill());
        assertEquals("reading-foundation-01", service.search("reading").get(0).id());
    }

    @Test
    void rejectsBlankOrOversizedQueries() {
        PracticeSearchService service = new PracticeSearchService();

        assertThrows(IllegalArgumentException.class, () -> service.search(" "));
        assertThrows(IllegalArgumentException.class, () -> service.search("a".repeat(121)));
    }
}
