package com.ieltsaitutor.mock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

class MockTestCatalogServiceTest {
    @Test
    void savesDraftThenPublishesItForLearnerCatalog() {
        InMemoryRepository repository = new InMemoryRepository();
        MockTestCatalogService service = new MockTestCatalogService(repository);
        var command = new MockTestCatalogService.Command("academic-2", "Academic Mock 2", "v1", false,
                List.of(new MockTestCatalogItem.Section(0, "READING", "set-reading", 3600)));

        MockTestCatalogItem draft = service.save(command);
        assertFalse(service.published().contains(draft));
        MockTestCatalogItem published = service.publish(draft.id(), true);
        assertTrue(service.published().contains(published));
        assertEquals(3600, published.totalTimeLimitSeconds());
    }

    @Test
    void updatesExistingDefinitionWithoutChangingItsIdentity() {
        InMemoryRepository repository = new InMemoryRepository();
        MockTestCatalogService service = new MockTestCatalogService(repository);
        MockTestCatalogItem created = service.save(new MockTestCatalogService.Command("academic-3", "Academic Mock 3", "v1", false,
                List.of(new MockTestCatalogItem.Section(0, "READING", "set-reading", 3600))));

        MockTestCatalogItem updated = service.update(created.id(), new MockTestCatalogService.Command("academic-3", "Academic Mock 3 Revised", "v2", true,
                List.of(
                        new MockTestCatalogItem.Section(0, "LISTENING", "set-listening", 1800),
                        new MockTestCatalogItem.Section(1, "READING", "set-reading", 3600),
                        new MockTestCatalogItem.Section(2, "WRITING", "set-writing", 3600),
                        new MockTestCatalogItem.Section(3, "SPEAKING", "set-speaking", 1800))));

        assertEquals(created.id(), updated.id());
        assertEquals("Academic Mock 3 Revised", updated.title());
        assertEquals(10800, updated.totalTimeLimitSeconds());
        assertEquals(4, updated.sections().size());
        assertTrue(service.published().contains(updated));
    }

    private static final class InMemoryRepository implements MockTestCatalogRepository {
        private final Map<UUID, MockTestCatalogItem> values = new LinkedHashMap<>();
        @Override public List<MockTestCatalogItem> findAll(boolean publishedOnly) { return values.values().stream().filter(item -> !publishedOnly || item.published()).toList(); }
        @Override public Optional<MockTestCatalogItem> findBySlug(String slug) { return values.values().stream().filter(item -> item.slug().equals(slug)).findFirst(); }
        @Override public Optional<MockTestCatalogItem> findById(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public MockTestCatalogItem save(MockTestCatalogItem item) { values.put(item.id(), item); return item; }
        @Override public void delete(UUID id) { values.remove(id); }
    }
}
