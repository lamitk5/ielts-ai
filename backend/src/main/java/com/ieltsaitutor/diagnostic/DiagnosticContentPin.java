package com.ieltsaitutor.diagnostic;

/** One approved, version-pinned content reference behind a diagnostic section. */
public record DiagnosticContentPin(String publishedSetId, String practiceVersionId,
        int publicationRevision, String provenanceReference) {

    public DiagnosticContentPin {
        if (publishedSetId == null || publishedSetId.isBlank()) {
            throw new IllegalArgumentException("Published set id is required");
        }
        if (practiceVersionId == null || practiceVersionId.isBlank()) {
            throw new IllegalArgumentException("Practice version id is required");
        }
        if (publicationRevision < 1) {
            throw new IllegalArgumentException("Publication revision must be positive");
        }
    }

    public static DiagnosticContentPin of(String publishedSetId, String practiceVersionId,
            int publicationRevision, String provenanceReference) {
        return new DiagnosticContentPin(publishedSetId, practiceVersionId, publicationRevision, provenanceReference);
    }
}