package com.ieltsaitutor.profile;

import java.time.LocalDate;

import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.onboarding.OnboardingState;

/**
 * UpdateProfileCommand is strictly bounded to learner-facing profile fields.
 * Any role, admin, authority, privilege, or password fields sent by clients
 * are disregarded by construction.
 */
public record UpdateProfileCommand(
        String firstName,
        String avatarUrl,
        Double targetBand,
        LocalDate targetExamDate,
        Skill perceivedWeakestSkill,
        Integer dailyStudyMinutes,
        Integer studyDaysPerWeek,
        String selfReportedLevel,
        OnboardingState onboardingState,
        Long onboardingVersion) {}
