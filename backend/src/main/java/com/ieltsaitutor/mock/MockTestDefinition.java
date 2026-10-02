package com.ieltsaitutor.mock;

import java.util.List;

public record MockTestDefinition(
        String id,
        String title,
        String version,
        int totalTimeLimitSeconds,
        List<MockSectionSpec> sectionSpecs) {

    public record MockSectionSpec(
            int order,
            String skill,
            String practiceId,
            String practiceVersionId,
            String publishedSetId,
            int timeLimitSeconds
    ) {}

    public static final MockTestDefinition DEFAULT_MOCK = new MockTestDefinition(
            "mock-test-academic-01",
            "IELTS Academic Full Mock Test 1",
            "v1",
            10800,
            List.of(
                    new MockSectionSpec(0, "LISTENING", "mock-listening-01", "v1", "default-listening-set", 1800),
                    new MockSectionSpec(1, "READING", "mock-reading-01", "v1", "default-reading-set", 3600),
                    new MockSectionSpec(2, "WRITING", "mock-writing-01", "v1", "default-writing-set", 3600),
                    new MockSectionSpec(3, "SPEAKING", "mock-speaking-01", "v1", "default-speaking-set", 900)
            )
    );
}
