package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DeterministicMistakeClassifierTest {
    private final DeterministicMistakeClassifier classifier = new DeterministicMistakeClassifier();

    @Test
    void readingAndListeningUseQuestionEvidenceBeforeHeuristics() {
        MistakeClassification reading = classifier.classify(new MistakeEvidence(Skill.READING, "TRUE_FALSE_NOT_GIVEN",
                "FALSE", "TRUE", false, "q1"));
        MistakeClassification listening = classifier.classify(new MistakeEvidence(Skill.LISTENING, "FORM_COMPLETION",
                "cat", "cats", false, "q2"));

        assertEquals("TRUE_NOT_GIVEN_CONFUSION", reading.category());
        assertEquals("PLURAL_SINGULAR", listening.category());
        assertEquals(MistakeMethod.DETERMINISTIC, reading.method());
    }

    @Test
    void writingAndSpeakingStayWithinTextOnlyTaxonomy() {
        assertEquals("GRAMMATICAL_RANGE_ACCURACY", classifier.classify(
                new MistakeEvidence(Skill.WRITING, "GRAMMAR", "fragment", "complete", false, "w1")).category());
        assertEquals("ANSWER_DEVELOPMENT", classifier.classify(
                new MistakeEvidence(Skill.SPEAKING, "DEVELOPMENT", "short", "expanded", false, "s1")).category());
    }

    @Test
    void unsupportedOrInsufficientEvidenceIsUnknown() {
        MistakeClassification result = classifier.classify(
                new MistakeEvidence(Skill.READING, "UNKNOWN_TYPE", "", "", false, "q3"));
        assertEquals("UNKNOWN_READING_ERROR", result.category());
        assertEquals(MistakeMethod.UNKNOWN, result.method());
    }
}
