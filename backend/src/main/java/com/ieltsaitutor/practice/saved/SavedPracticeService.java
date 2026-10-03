package com.ieltsaitutor.practice.saved;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;
import com.ieltsaitutor.practice.catalog.PracticePublication;

@Service
public class SavedPracticeService {
    private final SavedPracticeRepository repository;
    private final ApprovedPracticeCatalogService catalogService;

    public SavedPracticeService(SavedPracticeRepository repository, ApprovedPracticeCatalogService catalogService) {
        this.repository = repository;
        this.catalogService = catalogService;
    }

    public SavedPractice save(UUID userId, String publishedSetId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId là bắt buộc.");
        }
        if (publishedSetId == null || publishedSetId.isBlank()) {
            throw new IllegalArgumentException("publishedSetId là bắt buộc.");
        }

        String normalizedId = publishedSetId.trim();
        Optional<PracticePublication> publicationOpt = catalogService.findActive(normalizedId);

        String skill = "reading";
        String title = normalizedId;

        if (publicationOpt.isPresent()) {
            PracticePublication pub = publicationOpt.get();
            if (!pub.active()) {
                throw new IllegalArgumentException("Chỉ có thể lưu bài luyện đã được duyệt và đang hoạt động.");
            }
            skill = pub.skill();
            title = "Bài luyện IELTS " + skill.toUpperCase(Locale.ROOT) + " (" + normalizedId + ")";
        } else {
            // Check known approved synthetic catalog sets
            if (isApprovedSynthetic(normalizedId)) {
                skill = resolveSyntheticSkill(normalizedId);
                title = resolveSyntheticTitle(normalizedId);
            } else {
                throw new IllegalArgumentException("Chỉ có thể lưu bài luyện đã được duyệt và đang hoạt động.");
            }
        }

        SavedPractice item = new SavedPractice(
                UUID.randomUUID(),
                userId,
                normalizedId,
                skill,
                title,
                Instant.now(),
                true
        );
        return repository.save(item);
    }

    public void unsave(UUID userId, String publishedSetId) {
        if (userId == null || publishedSetId == null || publishedSetId.isBlank()) {
            return;
        }
        repository.delete(userId, publishedSetId.trim());
    }

    public List<SavedPractice> list(UUID userId, String skillFilter, int page, int size) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByUser(userId, skillFilter, page, size);
    }

    public boolean isSaved(UUID userId, String publishedSetId) {
        if (userId == null || publishedSetId == null || publishedSetId.isBlank()) {
            return false;
        }
        return repository.isSaved(userId, publishedSetId.trim());
    }

    private boolean isApprovedSynthetic(String id) {
        return id.startsWith("reading-foundation")
                || id.startsWith("listening-foundation")
                || id.startsWith("task-1-academic")
                || id.startsWith("task-2-opinion")
                || id.startsWith("speaking-p");
    }

    private String resolveSyntheticSkill(String id) {
        if (id.contains("reading")) return "reading";
        if (id.contains("listening")) return "listening";
        if (id.contains("writing") || id.startsWith("task-")) return "writing";
        if (id.contains("speaking")) return "speaking";
        return "general";
    }

    private String resolveSyntheticTitle(String id) {
        if (id.contains("reading")) return "Reading foundation";
        if (id.contains("listening")) return "Listening foundation";
        if (id.contains("task-1")) return "Academic Writing Task 1";
        if (id.contains("task-2")) return "Essay Writing Task 2";
        if (id.contains("speaking")) return "Speaking Part Practice";
        return id;
    }
}
