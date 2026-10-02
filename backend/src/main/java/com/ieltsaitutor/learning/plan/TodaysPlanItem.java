package com.ieltsaitutor.learning.plan;

import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.Skill;

public record TodaysPlanItem(UUID id, Skill skill, String practiceId, String title, int durationMinutes,
        TodaysPlanReason reasonCode, String reason, String evidenceReference, boolean targeted, String route) {
    public TodaysPlanItem {
        if (title == null || title.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("plan item requires title and reason");
        }
        if (durationMinutes < 1 || durationMinutes > 240) throw new IllegalArgumentException("invalid duration");
        route = route == null || route.isBlank() ? "/practice" : route;
    }
}
