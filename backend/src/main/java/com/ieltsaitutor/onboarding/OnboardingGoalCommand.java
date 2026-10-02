package com.ieltsaitutor.onboarding;

import java.time.LocalDate;

import com.ieltsaitutor.learning.intelligence.Skill;

public record OnboardingGoalCommand(
        String selfReportedLevel,
        Double targetBand,
        LocalDate targetExamDate,
        Skill perceivedWeakestSkill,
        Integer dailyStudyMinutes,
        Integer studyDaysPerWeek,
        OnboardingState state) {
}