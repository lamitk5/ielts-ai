package com.ieltsaitutor.diagnostic;

/** The Phase 4 submission backing one started diagnostic section. */
public record DiagnosticSectionStart(String skill, java.util.UUID submissionId, String publishedSetId,
        String practiceVersionId, int publicationRevision) {
}