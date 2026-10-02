package com.ieltsaitutor.writing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WritingRubricPolicyTest {

    @Test
    @DisplayName("Task 1 produces Task Achievement, Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy")
    void task1CriteriaMatchSpecification() {
        WritingTaskRubric rubric = WritingRubricPolicy.resolveRubric("task-1-bar-chart");
        assertEquals("TASK_1", rubric.taskType());
        assertEquals(List.of("taskAchievement", "coherenceCohesion", "lexicalResource", "grammaticalRangeAccuracy"), rubric.criterionKeys());
        assertTrue(rubric.hasCriterion("taskAchievement"));
        assertFalse(rubric.hasCriterion("taskResponse"));
    }

    @Test
    @DisplayName("Task 2 produces Task Response, Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy")
    void task2CriteriaMatchSpecification() {
        WritingTaskRubric rubric = WritingRubricPolicy.resolveRubric("task-2-technology-essay");
        assertEquals("TASK_2", rubric.taskType());
        assertEquals(List.of("taskResponse", "coherenceCohesion", "lexicalResource", "grammaticalRangeAccuracy"), rubric.criterionKeys());
        assertTrue(rubric.hasCriterion("taskResponse"));
        assertFalse(rubric.hasCriterion("taskAchievement"));
    }

    @Test
    @DisplayName("Unknown task throws IllegalArgumentException")
    void unknownTaskThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> WritingRubricPolicy.resolveRubric("speaking-part-1"));
        assertThrows(IllegalArgumentException.class, () -> WritingRubricPolicy.resolveRubric(null));
    }

    @Test
    @DisplayName("Validates criterion keys against rubric policy")
    void validatesCriteriaStrictly() {
        WritingTaskRubric task1Rubric = WritingRubricPolicy.resolveRubric("task-1-map");
        Map<String, String> validTask1Criteria = Map.of(
                "taskAchievement", "Good overview",
                "coherenceCohesion", "Logical flow",
                "lexicalResource", "Rich vocabulary",
                "grammaticalRangeAccuracy", "Complex sentences");
        assertTrue(task1Rubric.isValidCriteria(validTask1Criteria));

        Map<String, String> invalidCrossContaminated = Map.of(
                "taskResponse", "Wrong criterion for task 1",
                "coherenceCohesion", "Logical flow",
                "lexicalResource", "Rich vocabulary",
                "grammaticalRangeAccuracy", "Complex sentences");
        assertFalse(task1Rubric.isValidCriteria(invalidCrossContaminated));
    }
}
