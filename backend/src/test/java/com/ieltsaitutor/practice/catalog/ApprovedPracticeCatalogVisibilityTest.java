package com.ieltsaitutor.practice.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApprovedPracticeCatalogVisibilityTest {

    @Test
    void learnerCatalogContainsOnlyActiveApprovedPublications() {
        FakePublicationRepository repository = new FakePublicationRepository(List.of(
                publication("reading-active", "reading", true),
                publication("reading-inactive", "reading", false),
                publication("listening-active", "listening", true)));
        ApprovedPracticeCatalogService service = new ApprovedPracticeCatalogService(repository);

        assertEquals(List.of("reading-active"), service.listActive("reading").stream()
                .map(PracticePublication::publishedSetId).toList());
        assertTrue(service.findActive("reading-inactive").isEmpty());
    }

    @Test
    void publicationUpsertKeepsVersionAndProvenanceIdentity() {
        FakePublicationRepository repository = new FakePublicationRepository(List.of());
        ApprovedPracticeCatalogService service = new ApprovedPracticeCatalogService(repository);
        PracticePublication expected = publication("reading-versioned", "reading", true);

        assertEquals(expected, service.publishApproved(expected));
        assertEquals(expected.generatedVersionId(), repository.findActiveById(expected.publishedSetId()).orElseThrow().generatedVersionId());
        assertEquals(expected.provenanceReference(), repository.findActiveById(expected.publishedSetId()).orElseThrow().provenanceReference());
    }

    private static PracticePublication publication(String id, String skill, boolean active) {
        return new PracticePublication(id, UUID.randomUUID(), UUID.randomUUID(), skill, active, 1, "provenance-" + id, Instant.now());
    }

    private static final class FakePublicationRepository implements PracticePublicationRepository {
        private final List<PracticePublication> publications;

        private FakePublicationRepository(List<PracticePublication> publications) {
            this.publications = new ArrayList<>(publications);
        }

        @Override
        public PracticePublication save(PracticePublication publication) {
            publications.removeIf(existing -> existing.publishedSetId().equals(publication.publishedSetId()));
            publications.add(publication);
            return publication;
        }

        @Override
        public List<PracticePublication> findActiveBySkill(String skill) {
            return publications.stream().filter(item -> item.active() && item.skill().equalsIgnoreCase(skill)).toList();
        }

        @Override
        public Optional<PracticePublication> findActiveById(String publishedSetId) {
            return publications.stream().filter(item -> item.active() && item.publishedSetId().equals(publishedSetId)).findFirst();
        }

        @Override
        public void deactivate(String publishedSetId) {
            for (int index = 0; index < publications.size(); index++) {
                PracticePublication item = publications.get(index);
                if (item.publishedSetId().equals(publishedSetId)) {
                    publications.set(index, new PracticePublication(item.publishedSetId(), item.generatedSetId(), item.generatedVersionId(), item.skill(), false, item.publicationRevision(), item.provenanceReference(), item.publishedAt()));
                }
            }
        }
    }
}
