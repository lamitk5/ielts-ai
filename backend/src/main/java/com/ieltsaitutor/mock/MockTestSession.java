package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record MockTestSession(
        UUID id,
        UUID userId,
        String mockTestId,
        String mockTestVersion,
        MockTestSessionStatus status,
        int currentSectionIndex,
        int totalTimeLimitSeconds,
        int elapsedSeconds,
        Instant startedAt,
        Instant expiresAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<MockTestSection> sections) {

    public MockTestSession {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(mockTestId, "mockTestId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        mockTestVersion = mockTestVersion != null && !mockTestVersion.isBlank() ? mockTestVersion.trim() : "v1";
        if (currentSectionIndex < 0) throw new IllegalArgumentException("currentSectionIndex cannot be negative");
        if (totalTimeLimitSeconds <= 0) throw new IllegalArgumentException("totalTimeLimitSeconds must be positive");
        if (elapsedSeconds < 0) throw new IllegalArgumentException("elapsedSeconds cannot be negative");
        sections = sections != null ? List.copyOf(sections) : Collections.emptyList();
    }

    public MockTestSession withStatus(MockTestSessionStatus newStatus) {
        return new MockTestSession(
                id, userId, mockTestId, mockTestVersion, newStatus, currentSectionIndex,
                totalTimeLimitSeconds, elapsedSeconds, startedAt, expiresAt, completedAt, createdAt, Instant.now(), sections
        );
    }

    public MockTestSession withCurrentSection(int nextIndex) {
        return new MockTestSession(
                id, userId, mockTestId, mockTestVersion, status, nextIndex,
                totalTimeLimitSeconds, elapsedSeconds, startedAt, expiresAt, completedAt, createdAt, Instant.now(), sections
        );
    }

    public MockTestSession withTiming(int elapsed, Instant expires) {
        return new MockTestSession(
                id, userId, mockTestId, mockTestVersion, status, currentSectionIndex,
                totalTimeLimitSeconds, elapsed, startedAt, expires, completedAt, createdAt, Instant.now(), sections
        );
    }

    public MockTestSession withSections(List<MockTestSection> updatedSections) {
        return new MockTestSession(
                id, userId, mockTestId, mockTestVersion, status, currentSectionIndex,
                totalTimeLimitSeconds, elapsedSeconds, startedAt, expiresAt, completedAt, createdAt, Instant.now(), updatedSections
        );
    }
}
