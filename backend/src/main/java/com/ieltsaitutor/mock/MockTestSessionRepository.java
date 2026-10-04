package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockTestSessionRepository {
    MockTestSession save(MockTestSession session);
    Optional<MockTestSession> findById(UUID id);
    Optional<MockTestSession> findByUserAndId(UUID userId, UUID id);
    Optional<MockTestSession> findActiveByUser(UUID userId);
    List<MockTestSession> findByUser(UUID userId);
    void updateStatus(UUID id, MockTestSessionStatus status, Instant completedAt);
}
