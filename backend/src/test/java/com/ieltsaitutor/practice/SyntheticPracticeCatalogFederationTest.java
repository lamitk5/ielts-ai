package com.ieltsaitutor.practice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.repository.DatabasePracticeCatalogStore;

class SyntheticPracticeCatalogFederationTest {

    private DatabasePracticeCatalogStore dbStore;
    private SyntheticPracticeCatalog catalog;

    @BeforeEach
    void setUp() {
        dbStore = new DatabasePracticeCatalogStore();
        catalog = new SyntheticPracticeCatalog(dbStore);
    }

    @Test
    void federatesHardcodedAndDatabaseApprovedSets() {
        assertEquals(1, catalog.sets("reading").size());
        assertEquals(1, catalog.sets("listening").size());

        dbStore.save(new PracticeSet(
                "reading-synth-99", "reading", "Federated Reading", "Desc", List.of(), null
        ));

        List<PracticeSet> readingSets = catalog.sets("reading");
        assertEquals(2, readingSets.size());
        assertTrue(readingSets.stream().anyMatch(s -> s.id().equals("reading-foundation-01")));
        assertTrue(readingSets.stream().anyMatch(s -> s.id().equals("reading-synth-99")));

        PracticeSet found = catalog.find("reading", "reading-synth-99");
        assertEquals("Federated Reading", found.title());
    }

    @Test
    void skillValidationIsStrict() {
        assertThrows(IllegalArgumentException.class, () -> catalog.sets("writing"));
        assertThrows(IllegalArgumentException.class, () -> catalog.sets("unknown"));
    }
}
