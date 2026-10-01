package com.ieltsaitutor.learning.intelligence;

public record AdaptiveLearningProperties(int recentWindowDays, int minimumAttempts, int minimumEvaluatedItems,
        int emergingMinOccurrences, int confirmedMinOccurrences, int confirmedMinAttempts, double confirmedRate,
        int improvementWindows) {
    public static AdaptiveLearningProperties defaults() {
        return new AdaptiveLearningProperties(30, 3, 10, 2, 3, 2, .20, 2);
    }
}
