package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;

public record HistoricalEvidence(String sourceId, Skill skill, String practiceSetId, int score, int total, Instant occurredAt) {}
