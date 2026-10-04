package com.ieltsaitutor.diagnostic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticSessionRepository {
    DiagnosticSession create(DiagnosticSession session);

    Optional<DiagnosticSession> find(UUID userId, UUID sessionId);

    Optional<DiagnosticSession> findActive(UUID userId);

    boolean update(DiagnosticSession session, long expectedVersion);

    List<DiagnosticSession> history(UUID userId, int limit);

    int nextAttemptNumber(UUID userId);
}