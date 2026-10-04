package com.ieltsaitutor.learning.vocabulary;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyService {
    private final VocabularyRepository repository;
    private final Clock clock;
    @Autowired
    public VocabularyService(VocabularyRepository repository) { this(repository, Clock.systemUTC()); }
    public VocabularyService(VocabularyRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }

    public List<VocabularyItem> list(UUID userId, String query, VocabularyStatus status) { return repository.findByUser(userId, query, status, "created"); }
    public VocabularyItem create(UUID userId, SaveCommand command) {
        validate(command);
        Instant now = clock.instant();
        return repository.save(new VocabularyItem(UUID.randomUUID(), userId, command.word(), null, command.meaning(),
                command.exampleSentence(), command.note(), command.source(), command.sourceReferenceId(), VocabularyStatus.NEW, now, now, null, 0));
    }
    public VocabularyItem update(UUID userId, UUID id, SaveCommand command) {
        validate(command);
        VocabularyItem old = owned(userId, id);
        return repository.save(new VocabularyItem(old.id(), old.userId(), command.word(), null, command.meaning(), command.exampleSentence(),
                command.note(), command.source(), command.sourceReferenceId(), old.status(), old.createdAt(), clock.instant(), old.lastReviewedAt(), old.reviewCount()));
    }
    @Transactional public VocabularyItem review(UUID userId, UUID id, VocabularyStatus status) {
        VocabularyItem old = owned(userId, id);
        Instant now = clock.instant();
        return repository.save(new VocabularyItem(old.id(), old.userId(), old.word(), old.normalizedWord(), old.meaning(), old.exampleSentence(), old.note(),
                old.source(), old.sourceReferenceId(), status == null ? VocabularyStatus.LEARNING : status, old.createdAt(), now, now, old.reviewCount() + 1));
    }
    public void delete(UUID userId, UUID id) { owned(userId, id); repository.delete(userId, id); }
    private VocabularyItem owned(UUID userId, UUID id) { return repository.findByIdAndUser(userId, id).orElseThrow(() -> new NoSuchElementException("Không tìm thấy từ vựng.")); }
    private void validate(SaveCommand c) { if (c == null || c.word() == null || c.word().isBlank() || c.word().length() > 180 || c.meaning() == null || c.meaning().isBlank()) throw new IllegalArgumentException("Từ và nghĩa là bắt buộc."); }
    public record SaveCommand(String word, String meaning, String exampleSentence, String note, String source, String sourceReferenceId) {
        public SaveCommand(String word, String meaning, String exampleSentence, String source, String sourceReferenceId) {
            this(word, meaning, exampleSentence, null, source, sourceReferenceId);
        }
    }
}
