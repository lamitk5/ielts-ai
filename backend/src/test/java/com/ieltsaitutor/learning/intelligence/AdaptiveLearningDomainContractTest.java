package com.ieltsaitutor.learning.intelligence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AdaptiveLearningDomainContractTest {
    @Test
    void learningEventCapturesTrustedBoundedEvidenceMetadata() {
        UUID userId = UUID.randomUUID();
        LearningEvent event = new LearningEvent(UUID.randomUUID(), userId, LearningEventType.ANSWER_SUBMITTED,
                Skill.READING, null, "reading-set-1", UUID.randomUUID(), "q1", null,
                "practice:attempt:q1", Map.of("selectedAnswer", "B"), "client-1",
                Instant.now(), Instant.now());

        assertEquals(userId, event.userId());
        assertEquals(Skill.READING, event.skill());
        assertEquals("B", event.payload().get("selectedAnswer"));
    }

    @Test
    void profilesKeepEstimateNullableAndExposeInsufficientEvidence() {
        UUID userId = UUID.randomUUID();
        StudentLearningProfile profile = StudentLearningProfile.empty(userId, Instant.now());
        StudentSkillProfile skill = StudentSkillProfile.empty(userId, Skill.WRITING, Instant.now());

        assertEquals(EvidenceState.INSUFFICIENT_DATA, profile.evidenceState());
        assertNull(skill.latestBandEstimate());
        assertEquals(EvidenceState.INSUFFICIENT_DATA, skill.evidenceState());
    }

    @Test
    void roadmapItemPreservesEvidenceLinkAndBoundedPriority() {
        LearningRoadmapItem item = new LearningRoadmapItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Skill.LISTENING, "Recognize paraphrase", "TARGETED_PRACTICE", "MISSED_PARAPHRASE", "DETAIL",
                1, "SHORT", RoadmapItemStatus.NOT_STARTED, "RECURRENT_MISTAKE", UUID.randomUUID(),
                Map.of("occurrences", 3));

        assertEquals(Skill.LISTENING, item.skill());
        assertEquals(1, item.priority());
        assertEquals("RECURRENT_MISTAKE", item.reasonCode());
    }
}
