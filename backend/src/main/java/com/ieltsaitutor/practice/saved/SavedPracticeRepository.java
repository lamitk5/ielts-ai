package com.ieltsaitutor.practice.saved;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedPracticeRepository {
    SavedPractice save(SavedPractice savedPractice);

    void delete(UUID userId, String publishedSetId);

    List<SavedPractice> findByUser(UUID userId, String skillFilter, int page, int size);

    Optional<SavedPractice> findByUserAndSetId(UUID userId, String publishedSetId);

    boolean isSaved(UUID userId, String publishedSetId);

    long countByUser(UUID userId);
}
