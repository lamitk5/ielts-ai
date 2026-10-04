package com.ieltsaitutor.learning.vocabulary;

import java.time.Instant;
import java.util.UUID;

public record VocabularyItem(
        UUID id, UUID userId, String word, String normalizedWord, String meaning,
        String exampleSentence, String note, String source, String sourceReferenceId,
        VocabularyStatus status, Instant createdAt, Instant updatedAt,
        Instant lastReviewedAt, int reviewCount) {
    public VocabularyItem {
        if (id == null || userId == null || word == null || word.isBlank() || meaning == null || meaning.isBlank()) {
            throw new IllegalArgumentException("word, meaning and ownership are required");
        }
        word = word.trim();
        normalizedWord = normalizedWord == null || normalizedWord.isBlank()
                ? word.toLowerCase(java.util.Locale.ROOT) : normalizedWord.trim().toLowerCase(java.util.Locale.ROOT);
        status = status == null ? VocabularyStatus.NEW : status;
        source = source == null || source.isBlank() ? "manual" : source.trim();
        reviewCount = Math.max(0, reviewCount);
    }
}
