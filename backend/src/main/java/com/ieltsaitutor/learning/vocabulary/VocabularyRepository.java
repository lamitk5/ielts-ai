package com.ieltsaitutor.learning.vocabulary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabularyRepository {
    List<VocabularyItem> findByUser(UUID userId, String query, VocabularyStatus status, String sort);
    Optional<VocabularyItem> findByIdAndUser(UUID userId, UUID id);
    VocabularyItem save(VocabularyItem item);
    void delete(UUID userId, UUID id);
}
