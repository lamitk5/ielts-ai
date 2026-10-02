package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WritingVersionRepositoryTest {

    @Test
    @DisplayName("In-memory or mock verification of WritingVersionRepository contract")
    void testVersionLifecycleAndOrdering() {
        WritingVersionRepository repo = new InMemoryWritingVersionRepository();
        UUID subId = UUID.randomUUID();

        WritingSubmissionVersion v1 = new WritingSubmissionVersion(
                UUID.randomUUID(), subId, 1, null, "First draft", 2, "hash1", Instant.now().minusSeconds(10));
        WritingSubmissionVersion v2 = new WritingSubmissionVersion(
                UUID.randomUUID(), subId, 2, v1.id(), "Second draft with more words", 6, "hash2", Instant.now());

        repo.save(v1);
        repo.save(v2);

        List<WritingSubmissionVersion> history = repo.findBySubmissionId(subId);
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).versionNumber()); // latest first
        assertEquals(1, history.get(1).versionNumber());

        Optional<WritingSubmissionVersion> found = repo.findBySubmissionIdAndVersionNumber(subId, 1);
        assertTrue(found.isPresent());
        assertEquals("First draft", found.get().responseText());
    }

    private static class InMemoryWritingVersionRepository implements WritingVersionRepository {
        private final java.util.Map<UUID, WritingSubmissionVersion> store = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public WritingSubmissionVersion save(WritingSubmissionVersion version) {
            store.put(version.id(), version);
            return version;
        }

        @Override
        public Optional<WritingSubmissionVersion> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<WritingSubmissionVersion> findBySubmissionId(UUID submissionId) {
            return store.values().stream()
                    .filter(v -> v.submissionId().equals(submissionId))
                    .sorted((a, b) -> Integer.compare(b.versionNumber(), a.versionNumber()))
                    .toList();
        }

        @Override
        public Optional<WritingSubmissionVersion> findBySubmissionIdAndVersionNumber(UUID submissionId, int versionNumber) {
            return store.values().stream()
                    .filter(v -> v.submissionId().equals(submissionId) && v.versionNumber() == versionNumber)
                    .findFirst();
        }

        @Override
        public int getNextVersionNumber(UUID submissionId) {
            return store.values().stream()
                    .filter(v -> v.submissionId().equals(submissionId))
                    .mapToInt(WritingSubmissionVersion::versionNumber)
                    .max()
                    .orElse(0) + 1;
        }
    }
}
