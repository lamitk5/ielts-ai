package com.ieltsaitutor.diagnostic;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.Skill;

/**
 * An owner-scoped, resumable diagnostic baseline. The content snapshot is fixed
 * when the session is created and never changes afterwards.
 */
public record DiagnosticSession(UUID id, UUID userId, DiagnosticState state, String definitionVersion,
        int attemptNumber, List<DiagnosticSectionPin> sections, Instant startedAt, Instant submittedAt,
        long version, Instant createdAt, Instant updatedAt) {

    public DiagnosticSession {
        if (id == null || userId == null || state == null || startedAt == null
                || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Diagnostic session identity is required");
        }
        if (definitionVersion == null || definitionVersion.isBlank()) {
            throw new IllegalArgumentException("Diagnostic definition version is required");
        }
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("Attempt number must be positive");
        }
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    public boolean isActive() {
        return state == DiagnosticState.IN_PROGRESS;
    }

    public boolean isFinalized() {
        return state == DiagnosticState.SUBMITTED || state == DiagnosticState.COMPLETED
                || state == DiagnosticState.SKIPPED;
    }

    public int startedSectionCount() {
        return (int) sections.stream().filter(DiagnosticSectionPin::started).count();
    }

    public boolean hasEvidence() {
        return startedSectionCount() > 0;
    }

    public DiagnosticSectionPin section(Skill skill) {
        return sections.stream().filter(section -> section.skill() == skill).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Diagnostic section is required: " + skill));
    }

    public DiagnosticSession withState(DiagnosticState newState, Instant submittedAt, Instant now) {
        return new DiagnosticSession(id, userId, newState, definitionVersion, attemptNumber, sections,
                startedAt, submittedAt, version + 1, createdAt, now);
    }

    public DiagnosticSession withSections(List<DiagnosticSectionPin> updated, Instant now) {
        return new DiagnosticSession(id, userId, state, definitionVersion, attemptNumber, updated,
                startedAt, submittedAt, version + 1, createdAt, now);
    }

    public DiagnosticSession withStateAndSections(DiagnosticState newState, List<DiagnosticSectionPin> updated,
            Instant submittedAt, Instant now) {
        return new DiagnosticSession(id, userId, newState, definitionVersion, attemptNumber, updated,
                startedAt, submittedAt, version + 1, createdAt, now);
    }
}
