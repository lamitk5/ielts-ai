package com.ieltsaitutor.learning.plan;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.*;
import com.ieltsaitutor.onboarding.LearnerOnboardingProfile;
import com.ieltsaitutor.onboarding.OnboardingState;

class StudyPlanServiceTest {
    @Test
    void prioritizesWeakestMeasuredSkillAndExplainsWhy() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        LearnerOnboardingProfile profile = new LearnerOnboardingProfile(userId, "INTERMEDIATE", 7.0,
                LocalDate.of(2026, 12, 15), null, 45, 5, OnboardingState.COMPLETED,
                "SELF_REPORTED", "LEARNER_DECLARATION", null, null, null, 1, now, now);
        List<StudentSkillProfile> skills = List.of(
                skill(userId, Skill.READING, .51), skill(userId, Skill.LISTENING, .84),
                skill(userId, Skill.WRITING, .72), skill(userId, Skill.SPEAKING, .68));

        StudyPlan plan = new StudyPlanService().build(profile, skills, List.of(), 0, LocalDate.of(2026, 10, 4));

        assertEquals(7.0, plan.targetBand());
        assertEquals("READING", plan.recommendations().get(0).skill());
        assertTrue(plan.recommendations().get(0).reason().contains("weakest"));
        assertEquals(72, plan.daysRemaining());
    }

    @Test
    void doesNotInventMeasuredBandWithoutEvidence() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        LearnerOnboardingProfile profile = LearnerOnboardingProfile.empty(userId, now);
        StudyPlan plan = new StudyPlanService().build(profile, List.of(), List.of(), 0, LocalDate.of(2026, 10, 4));

        assertNull(plan.currentEstimatedBand());
        assertTrue(plan.recommendations().isEmpty());
    }

    private static StudentSkillProfile skill(UUID userId, Skill skill, double accuracy) {
        return new StudentSkillProfile(userId, skill, 3, 10, accuracy, null,
                EvidenceState.OBSERVATION, "STABLE", 0, 1, Instant.now(), Instant.now());
    }
}
