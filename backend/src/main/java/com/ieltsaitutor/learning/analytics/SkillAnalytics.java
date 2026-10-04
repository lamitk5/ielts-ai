package com.ieltsaitutor.learning.analytics;

public record SkillAnalytics(String skill, int completedAttempts, int evaluatedItems, Double accuracyPercent, Double latestBand, String state) {}
