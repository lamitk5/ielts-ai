package com.ieltsaitutor.tutor.proactive;

import java.time.Instant;
import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.Skill;

public record ProactiveTutorSuggestion(UUID id, UUID userId, UUID issueId, Skill skill,
        String title, String actionLabel, String reasonCode, Instant createdAt) {}
