package com.ieltsaitutor.mock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockTestSectionRepository {
    MockTestSection save(MockTestSection section);
    List<MockTestSection> saveAll(List<MockTestSection> sections);
    List<MockTestSection> findBySessionId(UUID sessionId);
    Optional<MockTestSection> findBySessionIdAndOrder(UUID sessionId, int sectionOrder);
    Optional<MockTestSection> findBySubmissionId(UUID submissionId);
}
