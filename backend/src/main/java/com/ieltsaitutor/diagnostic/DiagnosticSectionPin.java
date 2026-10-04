package com.ieltsaitutor.diagnostic;

import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.Skill;

/**
 * The content snapshot captured when a diagnostic starts. The pin is immutable
 * for the lifetime of the session so a retake measures the same content, and a
 * section can never be silently re-resolved to newer material.
 */
public record DiagnosticSectionPin(Skill skill, DiagnosticAvailability availability, String publishedSetId,
        String practiceVersionId, int publicationRevision, String provenanceReference, String reason,
        UUID submissionId, boolean started) {

    public DiagnosticSectionPin {
        if (skill == null) {
            throw new IllegalArgumentException("Skill is required");
        }
        if (availability == null) {
            throw new IllegalArgumentException("Availability is required");
        }
    }

    public static DiagnosticSectionPin available(Skill skill, DiagnosticContentPin content) {
        return new DiagnosticSectionPin(skill, DiagnosticAvailability.AVAILABLE, content.publishedSetId(),
                content.practiceVersionId(), content.publicationRevision(),
                content.provenanceReference(), null, null, false);
    }

    public static DiagnosticSectionPin unavailable(Skill skill, String reason) {
        return new DiagnosticSectionPin(skill, DiagnosticAvailability.CAPABILITY_UNAVAILABLE, null, null, 0,
                null, reason, null, false);
    }

    public DiagnosticSectionPin withSubmission(UUID submissionId) {
        return new DiagnosticSectionPin(skill, availability, publishedSetId, practiceVersionId,
                publicationRevision, provenanceReference, reason, submissionId, true);
    }

    public DiagnosticSectionPin resetForRetake() {
        return new DiagnosticSectionPin(skill, availability, publishedSetId, practiceVersionId,
                publicationRevision, provenanceReference, reason, null, false);
    }

    public boolean isAnswerable() {
        return availability == DiagnosticAvailability.AVAILABLE && publishedSetId != null;
    }
}
