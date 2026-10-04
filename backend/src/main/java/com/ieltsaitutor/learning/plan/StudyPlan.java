package com.ieltsaitutor.learning.plan;

import java.time.LocalDate;
import java.util.List;

public record StudyPlan(Double currentEstimatedBand, Double targetBand, LocalDate examDate,
        long daysRemaining, List<StudyPlanRecommendation> recommendations) {
    public StudyPlan { recommendations = recommendations == null ? List.of() : List.copyOf(recommendations); }
}
