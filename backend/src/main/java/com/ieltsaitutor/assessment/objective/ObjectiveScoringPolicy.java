package com.ieltsaitutor.assessment.objective;

public record ObjectiveScoringPolicy(String version) {
    public static final String CURRENT_VERSION = "objective-v1";

    public ObjectiveScoringPolicy {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("Scoring policy version is required");
    }

    public static ObjectiveScoringPolicy current() { return new ObjectiveScoringPolicy(CURRENT_VERSION); }
}
