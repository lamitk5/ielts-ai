package com.ieltsaitutor.diagnostic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiagnosticSectionRepository {
    DiagnosticSectionResult save(DiagnosticSectionResult result);
    Optional<DiagnosticSectionResult> findOwned(UUID userId, UUID sessionId, com.ieltsaitutor.learning.intelligence.Skill skill);
    List<DiagnosticSectionResult> findOwnedBySession(UUID userId, UUID sessionId);
}
