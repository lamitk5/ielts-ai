package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MockTestCatalogItem(UUID id, String slug, String title, String version, int totalTimeLimitSeconds,
        boolean published, Instant createdAt, Instant updatedAt, List<Section> sections) {
    public MockTestCatalogItem { sections = sections == null ? List.of() : List.copyOf(sections); }
    public record Section(int order, String skill, String practiceSetId, int timeLimitSeconds) {}
}
