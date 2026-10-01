package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SubmissionEngineIntegrationTest {
    @Test
    void durableLifecyclePinsVersionAutosavesAndFinalizesOnce() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        InMemorySubmissionRepository submissions = new InMemorySubmissionRepository();
        InMemoryDraftRepository drafts = new InMemoryDraftRepository();
        InMemoryAnswerRepository answers = new InMemoryAnswerRepository();
        SubmissionPracticeResolver resolver = mock(SubmissionPracticeResolver.class);
        when(resolver.resolve("reading-set", "reading"))
                .thenReturn(new ResolvedPracticeVersion("reading-set", "READING", "generated-v2", 7));

        CanonicalSubmissionService service = new CanonicalSubmissionService(submissions, resolver,
                new SubmissionDraftService(submissions, drafts),
                new SubmissionFinalizationService(submissions, answers, () -> Instant.now().plusSeconds(95)));

        PracticeSubmission startedSubmission = service.start(owner,
                new SubmissionStartCommand("reading-set", "reading", "start-1"));
        submissionId = startedSubmission.id();
        assertEquals(SubmissionStatus.IN_PROGRESS, startedSubmission.status());
        assertEquals("generated-v2", startedSubmission.practiceVersionId());
        assertEquals(7, startedSubmission.publicationRevision());
        assertSame(startedSubmission, service.start(owner,
                new SubmissionStartCommand("reading-set", "reading", "start-1")));

        SubmissionDraftSnapshot saved = service.autosave(owner, submissionId, Map.of("q1", "B"), 0, "draft-1");
        assertEquals(1, saved.revision());
        assertEquals(saved, service.autosave(owner, submissionId, Map.of("q1", "B"), 0, "draft-1"));
        assertEquals(1, service.get(owner, submissionId).autosaveRevision());

        PracticeSubmission submitted = service.submit(owner, submissionId, Map.of("q1", "B"), "submit-1");
        assertEquals(SubmissionStatus.SUBMITTED, submitted.status());
        assertEquals(95, submitted.durationSeconds());
        assertSame(submitted, service.submit(owner, submissionId, Map.of("q1", "B"), "submit-1"));
        assertEquals(1, answers.saved.size());

        SubmissionHistoryPage history = new SubmissionHistoryService(submissions).list(owner, "reading", null, 0, 20);
        assertEquals(1, history.total());
        assertEquals(submissionId, history.items().get(0).id());
    }

    private static final class InMemorySubmissionRepository implements PracticeSubmissionRepository {
        private PracticeSubmission current;

        @Override public PracticeSubmission create(PracticeSubmission submission) { current = submission; return submission; }
        @Override public Optional<PracticeSubmission> findById(UUID id) { return current != null && current.id().equals(id) ? Optional.of(current) : Optional.empty(); }
        @Override public Optional<PracticeSubmission> findByOwnerAndId(UUID ownerId, UUID id) { return findById(id).filter(item -> item.userId().equals(ownerId)); }
        @Override public Optional<PracticeSubmission> findByOwnerAndStartIdempotencyKey(UUID ownerId, String key) {
            return current == null || !current.userId().equals(ownerId) || !key.equals(current.startIdempotencyKey()) ? Optional.empty() : Optional.of(current);
        }
        @Override public Optional<PracticeSubmission> findByOwnerAndSubmitIdempotencyKey(UUID ownerId, String key) { return Optional.empty(); }
        @Override public PracticeSubmission updateStatus(UUID id, SubmissionStatus status, Instant updatedAt) {
            current = copy(status, current.startedAt(), current.lastSavedAt(), current.submittedAt(), current.scoredAt(),
                    current.autosaveRevision(), current.startIdempotencyKey(), current.submitIdempotencyKey(),
                    current.contentHash(), current.retryable(), updatedAt);
            return current;
        }
        @Override public Optional<PracticeSubmission> updateAutosaveIfRevision(UUID id, long expectedRevision, Instant savedAt) {
            if (current == null || current.autosaveRevision() != expectedRevision) return Optional.empty();
            current = copy(current.status(), current.startedAt(), savedAt, current.submittedAt(), current.scoredAt(),
                    expectedRevision + 1, current.startIdempotencyKey(), current.submitIdempotencyKey(), current.contentHash(),
                    current.retryable(), savedAt);
            return Optional.of(current);
        }
        @Override public Optional<PracticeSubmission> finalizeIfEditable(UUID id, String key, String hash, Instant submittedAt) {
            if (current == null || !current.editable()) return Optional.empty();
            current = copy(SubmissionStatus.SUBMITTED, current.startedAt(), submittedAt, submittedAt, current.scoredAt(),
                    current.autosaveRevision(), current.startIdempotencyKey(), key, hash, false, submittedAt);
            return Optional.of(current);
        }
        private PracticeSubmission copy(SubmissionStatus status, Instant startedAt, Instant lastSavedAt, Instant submittedAt,
                Instant scoredAt, long revision, String startKey, String submitKey, String hash, boolean retryable, Instant updatedAt) {
            return new PracticeSubmission(current.id(), current.userId(), current.skill(), current.practiceId(), current.practiceVersionId(),
                    current.publishedSetId(), current.publicationRevision(), status, startedAt, lastSavedAt, submittedAt, scoredAt,
                    revision, startKey, submitKey, hash, retryable, current.createdAt(), updatedAt);
        }
        @Override public SubmissionHistoryPage findHistory(UUID ownerId, String skill, SubmissionStatus status, int page, int size) {
            List<PracticeSubmission> items = current != null && current.userId().equals(ownerId)
                    && (skill == null || current.skill().equalsIgnoreCase(skill))
                    && (status == null || current.status() == status) ? List.of(current) : List.of();
            return new SubmissionHistoryPage(items, page, size, items.size());
        }
    }

    private static final class InMemoryDraftRepository implements SubmissionDraftRepository {
        private SubmissionDraftSnapshot current;
        @Override public Optional<SubmissionDraftSnapshot> findBySubmissionId(UUID id) { return Optional.ofNullable(current); }
        @Override public Optional<SubmissionDraftSnapshot> saveIfRevision(UUID id, UUID userId, Map<String, String> payload,
                long expectedRevision, String idempotencyKey, Instant updatedAt) {
            current = new SubmissionDraftSnapshot(id, userId, payload, expectedRevision + 1, idempotencyKey, updatedAt);
            return Optional.of(current);
        }
    }

    private static final class InMemoryAnswerRepository implements SubmissionAnswerRepository {
        private final List<SubmissionAnswerSnapshot> saved = new ArrayList<>();
        @Override public SubmissionAnswerSnapshot save(SubmissionAnswerSnapshot snapshot) { saved.add(snapshot); return snapshot; }
        @Override public Optional<SubmissionAnswerSnapshot> findBySubmissionId(UUID id) { return saved.stream().filter(item -> item.submissionId().equals(id)).findFirst(); }
    }
}
