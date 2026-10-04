package com.ieltsaitutor.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.onboarding.OnboardingState;

public record LearnerProfile(
        UUID id,
        String email,
        String firstName,
        String avatarUrl,
        String role,
        Double targetBand,
        LocalDate targetExamDate,
        Skill perceivedWeakestSkill,
        Integer dailyStudyMinutes,
        Integer studyDaysPerWeek,
        String selfReportedLevel,
        OnboardingState onboardingState,
        long onboardingVersion,
        Instant createdAt,
        Instant updatedAt) {}
