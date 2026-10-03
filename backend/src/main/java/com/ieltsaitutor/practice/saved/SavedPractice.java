package com.ieltsaitutor.practice.saved;

import java.time.Instant;
import java.util.UUID;

public record SavedPractice(
        UUID id,
        UUID userId,
        String publishedSetId,
        String skill,
        String title,
        Instant savedAt,
        boolean available) {

    public SavedPractice {
        if (id == null) id = UUID.randomUUID();
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (publishedSetId == null || publishedSetId.isBlank()) throw new IllegalArgumentException("publishedSetId is required");
        if (skill == null || skill.isBlank()) skill = "general";
        if (title == null || title.isBlank()) title = publishedSetId;
        if (savedAt == null) savedAt = Instant.now();
    }
}
