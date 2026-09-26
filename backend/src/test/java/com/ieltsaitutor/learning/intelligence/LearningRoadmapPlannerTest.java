package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LearningRoadmapPlannerTest {
    @Test
    void plannerCapsActivePrioritiesAtFiveAndUsesEvidence() {
        UUID userId = UUID.randomUUID();
        List<StudentLearningIssue> issues = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            issues.add(new StudentLearningIssue(UUID.randomUUID(), userId, IssueKind.WEAKNESS, Skill.READING,
                    "ERROR_" + index, EvidenceState.CONFIRMED, IssueStatus.OPEN, 1d, 3, 2, Instant.now(), "RECURRENT"));
        }

        LearningRoadmap roadmap = new LearningRoadmapPlanner().plan(userId, issues, Instant.now());

        assertEquals(5, roadmap.items().size());
        assertEquals("RECURRENT", roadmap.items().get(0).reasonCode());
    }
}
