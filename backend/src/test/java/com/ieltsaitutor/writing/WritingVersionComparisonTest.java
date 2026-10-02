package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WritingVersionComparisonTest {

    private WritingVersionComparisonService comparisonService;

    @BeforeEach
    void setUp() {
        comparisonService = new WritingVersionComparisonService();
    }

    @Test
    @DisplayName("Compares two text versions and detects paragraph changes")
    void detectsParagraphDifferences() {
        String text1 = "Paragraph one intro.\n\nParagraph two body.";
        String text2 = "Paragraph one intro modified.\n\nParagraph two body.\n\nParagraph three conclusion.";

        WritingSubmissionVersion v1 = new WritingSubmissionVersion(
                UUID.randomUUID(), UUID.randomUUID(), 1, null, text1, 7, "hash1", Instant.now().minusSeconds(100));
        WritingSubmissionVersion v2 = new WritingSubmissionVersion(
                UUID.randomUUID(), v1.submissionId(), 2, v1.id(), text2, 11, "hash2", Instant.now());

        WritingVersionComparisonView comparison = comparisonService.compare(v1, null, v2, null);

        assertEquals(1, comparison.baseVersionNumber());
        assertEquals(2, comparison.targetVersionNumber());
        assertEquals(3, comparison.targetParagraphs().size());
        assertEquals(2, comparison.baseParagraphs().size());
        assertFalse(comparison.evaluationsAvailable());
    }

    @Test
    @DisplayName("Compares two evaluated versions and tracks issues and criteria deltas without fabrication")
    void comparesEvaluatedVersionsAccurately() {
        WritingSubmissionVersion v1 = new WritingSubmissionVersion(
                UUID.randomUUID(), UUID.randomUUID(), 1, null, "Para 1", 2, "h1", Instant.now().minusSeconds(60));
        WritingSubmissionVersion v2 = new WritingSubmissionVersion(
                UUID.randomUUID(), v1.submissionId(), 2, v1.id(), "Para 1 updated", 3, "h2", Instant.now());

        WritingEvaluationResult e1 = new WritingEvaluationResult(
                UUID.randomUUID(), v1.id(), 1, 6.0,
                Map.of("taskResponse", "Adequate"),
                List.of("Clear viewpoint"),
                List.of("Weak conclusion", "Repetitive vocab"),
                List.of("Add summary"),
                List.of(), List.of(), "NOT_ENABLED",
                WritingEvaluationResult.STANDARD_DISCLAIMER, "GRADED", Instant.now().minusSeconds(60));

        WritingEvaluationResult e2 = new WritingEvaluationResult(
                UUID.randomUUID(), v2.id(), 1, 6.5,
                Map.of("taskResponse", "Well developed"),
                List.of("Clear viewpoint", "Effective conclusion"),
                List.of("Repetitive vocab", "Punctuation error"),
                List.of("Vary synonyms"),
                List.of(), List.of(), "NOT_ENABLED",
                WritingEvaluationResult.STANDARD_DISCLAIMER, "GRADED", Instant.now());

        WritingVersionComparisonView comparison = comparisonService.compare(v1, e1, v2, e2);

        assertTrue(comparison.evaluationsAvailable());
        assertEquals(0.5, comparison.bandDelta());
        assertEquals(List.of("Weak conclusion"), comparison.resolvedIssues());
        assertEquals(List.of("Repetitive vocab"), comparison.repeatedIssues());
        assertEquals(List.of("Punctuation error"), comparison.newIssues());
    }
}
