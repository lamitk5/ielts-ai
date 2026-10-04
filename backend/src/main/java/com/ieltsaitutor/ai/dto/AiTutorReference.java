package com.ieltsaitutor.ai.dto;

import java.util.Set;
import java.util.regex.Pattern;

public record AiTutorReference(
        String referenceType,
        String targetId,
        String questionId,
        String paragraphId,
        String sentenceId,
        Integer startOffset,
        Integer endOffset,
        String evidenceId,
        String severity,
        String label,
        Long contentVersion,
        Long draftVersion
) {
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "PASSAGE", "QUESTION", "OPTION", "DRAFT", "GENERAL", "EXPLANATION"
    );

    private static final Set<String> ALLOWED_SEVERITIES = Set.of(
            "INFO", "WARNING", "ERROR", "SUGGESTION"
    );

    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int MAX_OFFSET_SPAN = 50_000;

    public boolean isValid() {
        if (referenceType == null || !ALLOWED_TYPES.contains(referenceType.toUpperCase())) {
            return false;
        }
        if (targetId == null || !SAFE_ID_PATTERN.matcher(targetId).matches()) {
            return false;
        }
        if (questionId != null && !SAFE_ID_PATTERN.matcher(questionId).matches()) {
            return false;
        }
        if (paragraphId != null && !SAFE_ID_PATTERN.matcher(paragraphId).matches()) {
            return false;
        }
        if (sentenceId != null && !SAFE_ID_PATTERN.matcher(sentenceId).matches()) {
            return false;
        }
        if (severity != null && !ALLOWED_SEVERITIES.contains(severity.toUpperCase())) {
            return false;
        }

        boolean hasOffsets = startOffset != null || endOffset != null;
        if (hasOffsets) {
            if (startOffset == null || endOffset == null) return false;
            if (startOffset < 0 || endOffset < startOffset) return false;
            if (endOffset - startOffset > MAX_OFFSET_SPAN) return false;
            if ("DRAFT".equalsIgnoreCase(referenceType) && draftVersion == null && contentVersion == null) {
                return false;
            }
        }

        if (label != null && label.contains("<")) {
            return false;
        }

        return true;
    }
}
