package com.ieltsaitutor.learning.draft;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class LearningDraftService {
    private static final int MAX_CONTENT_LENGTH = 50_000;
    private final LearningDraftRepository repository;

    public LearningDraftService(LearningDraftRepository repository) {
        this.repository = repository;
    }

    public Optional<LearningDraft> getCurrentDraft(UUID userId, String skill, String referenceId) {
        if (userId == null || skill == null || referenceId == null) {
            return Optional.empty();
        }
        return repository.findActive(userId, skill.toUpperCase(), referenceId);
    }

    public LearningDraft saveDraft(UUID userId, String skill, String referenceId, String contentSnapshot, Long expectedVersion) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("Skill is required");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("Reference ID is required");
        }
        String normalizedContent = contentSnapshot == null ? "" : contentSnapshot;
        if (normalizedContent.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content snapshot exceeds maximum length of " + MAX_CONTENT_LENGTH);
        }

        String normalizedSkill = skill.toUpperCase();
        Optional<LearningDraft> existingOpt = repository.findActive(userId, normalizedSkill, referenceId);

        Instant now = Instant.now();
        if (existingOpt.isEmpty()) {
            LearningDraft newDraft = new LearningDraft(
                    UUID.randomUUID(),
                    userId,
                    normalizedSkill,
                    referenceId,
                    normalizedContent,
                    1L,
                    LearningDraft.DraftStatus.ACTIVE,
                    now,
                    now,
                    null
            );
            return repository.save(newDraft);
        }

        LearningDraft existing = existingOpt.get();
        if (expectedVersion != null && existing.version() != expectedVersion) {
            throw new DraftConflictException("VERSION_CONFLICT", "Bản nháp đã được cập nhật từ thiết bị khác.");
        }

        LearningDraft updatedDraft = new LearningDraft(
                existing.id(),
                userId,
                normalizedSkill,
                referenceId,
                normalizedContent,
                existing.version() + 1,
                LearningDraft.DraftStatus.ACTIVE,
                existing.createdAt(),
                now,
                null
        );

        return repository.save(updatedDraft);
    }

    public void deleteDraft(UUID userId, UUID draftId) {
        if (userId == null || draftId == null) return;
        Optional<LearningDraft> draftOpt = repository.findById(draftId);
        if (draftOpt.isPresent() && userId.equals(draftOpt.get().userId())) {
            repository.delete(draftId);
        }
    }
}
