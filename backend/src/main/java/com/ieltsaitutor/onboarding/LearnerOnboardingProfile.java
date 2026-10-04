package com.ieltsaitutor.onboarding;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.Skill;

/**
 * Owner-scoped self-reported goals and schedule. This record is a learner
 * declaration, never measured evidence: {@code source} is always
 * {@code SELF_REPORTED}, {@code measuredLevel}, {@code measuredWeakestSkill} and
 * {@code evidenceReference} stay null until a trusted Phase 4 graded result
 * supplies them. Nothing here is ever written into the adaptive evidence tables.
 */
public record LearnerOnboardingProfile(
        UUID userId,
        String selfReportedLevel,
        Double targetBand,
        LocalDate targetExamDate,
        Skill perceivedWeakestSkill,
        Integer dailyStudyMinutes,
        Integer studyDaysPerWeek,
        OnboardingState state,
        String source,
        String basis,
        String measuredLevel,
        Skill measuredWeakestSkill,
        String evidenceReference,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public boolean isGoalEstablished() { return state == OnboardingState.COMPLETED; }
    public boolean isSelfReportOnly() { return "SELF_REPORTED".equals(source); }

    public static LearnerOnboardingProfile empty(UUID userId, Instant now) {
        return new LearnerOnboardingProfile(userId, null, null, null, null, null, null,
                OnboardingState.NOT_STARTED, "SELF_REPORTED", "LEARNER_DECLARATION",
                null, null, null, 0L, now, now);
    }
}