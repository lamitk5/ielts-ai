package com.ieltsaitutor.learning.plan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ieltsaitutor.learning.intelligence.LearningEvent;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.learning.intelligence.StudentSkillProfile;
import com.ieltsaitutor.onboarding.LearnerOnboardingProfile;
import com.ieltsaitutor.profile.LearnerProfile;

public class StudyPlanService {
    public StudyPlan build(LearnerProfile profile, List<StudentSkillProfile> skills,
            List<LearningEvent> events, int vocabularyReviewCount, LocalDate today) {
        return build(profile == null ? null : new LearnerOnboardingProfile(profile.id(), profile.selfReportedLevel(),
                profile.targetBand(), profile.targetExamDate(), profile.perceivedWeakestSkill(), profile.dailyStudyMinutes(),
                profile.studyDaysPerWeek(), profile.onboardingState(), "SELF_REPORTED", "LEARNER_DECLARATION", null, null, null,
                profile.onboardingVersion(), profile.createdAt(), profile.updatedAt()), skills, events, vocabularyReviewCount, today);
    }

    public StudyPlan build(LearnerOnboardingProfile profile, List<StudentSkillProfile> skills,
            List<LearningEvent> events, int vocabularyReviewCount, LocalDate today) {
        List<StudentSkillProfile> measured = skills == null ? List.of() : skills.stream()
                .filter(item -> item.evidenceState() != null && item.evidenceState().name().equals("OBSERVATION"))
                .sorted(Comparator.comparingDouble(StudentSkillProfile::accuracyRate)).toList();
        Double currentBand = measured.stream().map(StudentSkillProfile::latestBandEstimate).filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).average().stream().boxed().findFirst().orElse(null);
        List<StudyPlanRecommendation> recommendations = new ArrayList<>();
        if (!measured.isEmpty()) {
            for (StudentSkillProfile item : measured.stream().limit(4).toList()) {
                String skill = item.skill().name();
                String reason = item == measured.get(0)
                        ? "Recommended because this is currently your weakest skill based on completed practice."
                        : "Recommended to keep your four-skill progress balanced.";
                recommendations.add(new StudyPlanRecommendation(skill, title(item.skill()), minutes(item.skill()), reason, route(item.skill())));
            }
        } else if (profile != null && profile.perceivedWeakestSkill() != null) {
            Skill skill = profile.perceivedWeakestSkill();
            recommendations.add(new StudyPlanRecommendation(skill.name(), title(skill), minutes(skill),
                    "Recommended because this is the skill you chose to prioritize.", route(skill)));
        }
        if (vocabularyReviewCount > 0) recommendations.add(new StudyPlanRecommendation("VOCABULARY", "Ôn lại từ vựng", 10,
                "Recommended because you have saved vocabulary ready for review.", "/vocabulary"));
        LocalDate examDate = profile == null ? null : profile.targetExamDate();
        long days = examDate == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(today, examDate));
        return new StudyPlan(currentBand, profile == null ? null : profile.targetBand(), examDate, days, recommendations);
    }
    private String title(Skill skill) { return switch (skill) { case READING -> "Luyện Reading"; case LISTENING -> "Luyện Listening"; case WRITING -> "Luyện Writing"; case SPEAKING -> "Luyện Speaking"; }; }
    private int minutes(Skill skill) { return skill == Skill.WRITING ? 40 : skill == Skill.SPEAKING ? 15 : 25; }
    private String route(Skill skill) { return "/practice/" + skill.name().toLowerCase(java.util.Locale.ROOT); }
}
