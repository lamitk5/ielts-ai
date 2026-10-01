package com.ieltsaitutor.submission;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class SubmissionHistoryService {
    private final PracticeSubmissionRepository repository;

    public SubmissionHistoryService(PracticeSubmissionRepository repository) {
        this.repository = repository;
    }

    public SubmissionHistoryPage list(UUID ownerId, String skill, String status, int page, int size) {
        if (ownerId == null) throw new SubmissionConflictException("Authentication is required");
        String normalizedSkill = normalizeSkill(skill);
        SubmissionStatus normalizedStatus = normalizeStatus(status);
        SubmissionHistoryQuery query = new SubmissionHistoryQuery(normalizedSkill, normalizedStatus, page, size);
        return repository.findHistory(ownerId, query.skill(), query.status(), query.page(), query.size());
    }

    private String normalizeSkill(String skill) {
        if (skill == null || skill.isBlank()) return null;
        String normalized = skill.trim().toLowerCase(Locale.ROOT);
        if (!SetOfSkills.contains(normalized)) throw new SubmissionConflictException("Invalid skill filter");
        return normalized;
    }

    private SubmissionStatus normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return SubmissionStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { throw new SubmissionConflictException("Invalid status filter"); }
    }

    private static final java.util.Set<String> SetOfSkills = java.util.Set.of("reading", "listening", "writing", "speaking");
}
