package com.ieltsaitutor.learning.vocabulary;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import org.junit.jupiter.api.Test;

class VocabularyServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final InMemoryVocabularyRepository repository = new InMemoryVocabularyRepository();
    private final VocabularyService service = new VocabularyService(
            repository, Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void createsOwnedWordWithNormalizedLookupKey() {
        VocabularyItem item = service.create(userId, new VocabularyService.SaveCommand(
                "  Resilient ", "able to recover", "Cities are resilient.", "Reading", "ref-1"));

        assertEquals(userId, item.userId());
        assertEquals("resilient", item.normalizedWord());
        assertEquals(VocabularyStatus.NEW, item.status());
        assertEquals(1, service.list(userId, "RES", null).size());
    }

    @Test
    void reviewsWordAndUpdatesMetadataForOwnerOnly() {
        VocabularyItem item = service.create(userId, new VocabularyService.SaveCommand(
                "coherent", "clear", "A coherent argument.", "manual", null));

        VocabularyItem reviewed = service.review(userId, item.id(), VocabularyStatus.MASTERED);

        assertEquals(VocabularyStatus.MASTERED, reviewed.status());
        assertEquals(1, reviewed.reviewCount());
        assertNotNull(reviewed.lastReviewedAt());
        assertThrows(NoSuchElementException.class,
                () -> service.review(UUID.randomUUID(), item.id(), VocabularyStatus.LEARNING));
    }

    private static final class InMemoryVocabularyRepository implements VocabularyRepository {
        private final Map<UUID, VocabularyItem> values = new LinkedHashMap<>();
        @Override public List<VocabularyItem> findByUser(UUID userId, String query, VocabularyStatus status, String sort) {
            return values.values().stream()
                    .filter(item -> item.userId().equals(userId))
                    .filter(item -> query == null || item.normalizedWord().contains(query.toLowerCase(Locale.ROOT)))
                    .filter(item -> status == null || item.status() == status)
                    .toList();
        }
        @Override public Optional<VocabularyItem> findByIdAndUser(UUID userId, UUID id) {
            return Optional.ofNullable(values.get(id)).filter(item -> item.userId().equals(userId));
        }
        @Override public VocabularyItem save(VocabularyItem item) { values.put(item.id(), item); return item; }
        @Override public void delete(UUID userId, UUID id) { values.remove(id); }
    }
}
