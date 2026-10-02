package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WritingEvaluationValidatorTest {

    private WritingEvaluationValidator validator;
    private WritingTaskRubric task1Rubric;
    private WritingTaskRubric task2Rubric;

    @BeforeEach
    void setUp() {
        validator = new WritingEvaluationValidator();
        task1Rubric = WritingRubricPolicy.resolveRubric("task-1-bar-chart");
        task2Rubric = WritingRubricPolicy.resolveRubric("task-2-essay");
    }

    @Test
    @DisplayName("Valid Task 1 JSON parses and validates correctly")
    void validTask1JsonParses() {
        String json = """
                {
                    "overallBandEstimate": 6.5,
                    "criteria": {
                        "taskAchievement": "Presents an overview of main trends",
                        "coherenceCohesion": "Organized paragraphs with linking devices",
                        "lexicalResource": "Adequate range of vocabulary",
                        "grammaticalRangeAccuracy": "Mix of simple and complex sentences"
                    },
                    "strengths": ["Clear overview paragraph"],
                    "issues": ["Minor inaccuracies in data comparison"],
                    "suggestions": ["Include specific data points for extremes"],
                    "evidenceSpans": ["The chart illustrates..."],
                    "priorityImprovements": ["Focus on key features first"]
                }
                """;

        WritingEvaluationResult result = validator.validateAndParse(json, task1Rubric);
        assertEquals("GRADED", result.status());
        assertEquals(6.5, result.overallBandEstimate());
        assertEquals(4, result.criteria().size());
        assertTrue(result.criteria().containsKey("taskAchievement"));
        assertEquals("Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.", result.disclaimer());
        assertEquals(1, result.strengths().size());
        assertEquals(1, result.issues().size());
    }

    @Test
    @DisplayName("Task 1 with Task 2 criteria is rejected as malformed")
    void task1WithTask2CriteriaRejected() {
        String json = """
                {
                    "overallBandEstimate": 7.0,
                    "criteria": {
                        "taskResponse": "Fully addressed all parts of the prompt",
                        "coherenceCohesion": "Well organized",
                        "lexicalResource": "Wide range of vocabulary",
                        "grammaticalRangeAccuracy": "Good grammar"
                    },
                    "strengths": ["Good position"],
                    "issues": [],
                    "suggestions": []
                }
                """;

        WritingEvaluationResult result = validator.validateAndParse(json, task1Rubric);
        assertEquals("MALFORMED", result.status());
        assertNull(result.overallBandEstimate());
    }

    @Test
    @DisplayName("Band out of bounds (<0 or >9) is rejected as malformed")
    void outOfBoundsBandRejected() {
        String json = """
                {
                    "overallBandEstimate": 10.5,
                    "criteria": {
                        "taskResponse": "Fully addressed",
                        "coherenceCohesion": "Good",
                        "lexicalResource": "Good",
                        "grammaticalRangeAccuracy": "Good"
                    }
                }
                """;

        WritingEvaluationResult result = validator.validateAndParse(json, task2Rubric);
        assertEquals("MALFORMED", result.status());
    }

    @Test
    @DisplayName("Malformed or non-JSON string returns MALFORMED status")
    void nonJsonReturnsMalformed() {
        WritingEvaluationResult result = validator.validateAndParse("Not a JSON string", task1Rubric);
        assertEquals("MALFORMED", result.status());
        assertNull(result.overallBandEstimate());
    }
}
