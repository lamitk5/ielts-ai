package com.ieltsaitutor.learning.analytics;

import java.util.List;

public record LearningAnalytics(List<SkillAnalytics> skills, List<QuestionTypeAnalytics> questionTypes,
        String strongestArea, String weakestArea, boolean hasEvidence) {
    public LearningAnalytics { skills = skills == null ? List.of() : List.copyOf(skills); questionTypes = questionTypes == null ? List.of() : List.copyOf(questionTypes); }
}
