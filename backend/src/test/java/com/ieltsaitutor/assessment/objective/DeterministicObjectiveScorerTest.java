package com.ieltsaitutor.assessment.objective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.PracticeQuestion;

class DeterministicObjectiveScorerTest {
    private final DeterministicObjectiveScorer scorer = new DeterministicObjectiveScorer();

    @Test
    void scoresMultipleChoiceAgainstServerAnswerKeyNotClientScore() {
        PracticeQuestion question = new PracticeQuestion("q1", "Which?", List.of("Alpha", "Beta"), "B", "why",
                "MULTIPLE_CHOICE", "passage:p1");

        ObjectiveScore result = scorer.score(List.of(question), Map.of("q1", "  b "));

        assertEquals(1, result.correctCount());
        assertTrue(result.questionResults().getFirst().correct());
        assertEquals("objective-v1", result.scoringPolicyVersion());
    }

    @Test
    void supportsCompletionCaseAndWhitespaceWithoutChangingUnknownSemantics() {
        PracticeQuestion completion = new PracticeQuestion("q1", "Fill", List.of(), "New York", "",
                "SUMMARY_COMPLETION", "p1");
        PracticeQuestion unknown = new PracticeQuestion("q2", "Exact", List.of(), "AB", "", "UNSUPPORTED", "p2");

        ObjectiveScore result = scorer.score(List.of(completion, unknown), Map.of("q1", " new   york ", "q2", " ab "));

        assertEquals(1, result.correctCount());
        assertTrue(result.questionResults().get(0).correct());
        assertFalse(result.questionResults().get(1).correct());
    }

    @Test
    void blankAnswerIsUnansweredAndNeverCorrect() {
        PracticeQuestion question = new PracticeQuestion("q1", "Which?", List.of("A"), "A", "");

        ObjectiveScore result = scorer.score(List.of(question), Map.of());

        assertEquals(0, result.correctCount());
        assertEquals("", result.questionResults().getFirst().normalizedLearnerAnswer());
    }
}
