package com.ieltsaitutor.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockTestDomainTest {

    @Test
    @DisplayName("MockTestSession validates required invariants and immutable lists")
    void sessionInvariants() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        MockTestSection sec = new MockTestSection(
                UUID.randomUUID(), sessionId, 0, "LISTENING", "mock-l1", "v1", "set-1",
                null, 1800, MockTestSectionStatus.NOT_STARTED, now, now
        );

        MockTestSession session = new MockTestSession(
                sessionId, userId, "mock-ielts-01", "v1", MockTestSessionStatus.NOT_STARTED,
                0, 10800, 0, null, null, null, now, now, List.of(sec)
        );

        assertThat(session.id()).isEqualTo(sessionId);
        assertThat(session.status().isMutable()).isTrue();
        assertThat(session.status().isTerminal()).isFalse();
        assertThat(session.sections()).hasSize(1);

        MockTestSession completed = session.withStatus(MockTestSessionStatus.COMPLETED);
        assertThat(completed.status().isTerminal()).isTrue();
        assertThat(completed.status().isMutable()).isFalse();
    }

    @Test
    @DisplayName("MockTestSection rejects negative order and enforces default version")
    void sectionInvariants() {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();

        assertThatThrownBy(() -> new MockTestSection(
                UUID.randomUUID(), sessionId, -1, "LISTENING", "mock-l1", "v1", "set-1",
                null, 1800, MockTestSectionStatus.NOT_STARTED, now, now
        )).isInstanceOf(IllegalArgumentException.class);

        MockTestSection section = new MockTestSection(
                UUID.randomUUID(), sessionId, 0, "READING", "mock-r1", null, null,
                null, 3600, MockTestSectionStatus.NOT_STARTED, now, now
        );
        assertThat(section.practiceVersionId()).isEqualTo("v1");
        assertThat(section.publishedSetId()).isEqualTo("default-set");
    }
}
