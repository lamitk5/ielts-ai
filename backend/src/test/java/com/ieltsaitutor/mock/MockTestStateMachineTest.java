package com.ieltsaitutor.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockTestStateMachineTest {

    private final MockTestStateMachine stateMachine = new MockTestStateMachine();

    private MockTestSession createSession(MockTestSessionStatus status) {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        List<MockTestSection> sections = List.of(
                new MockTestSection(UUID.randomUUID(), sessionId, 0, "LISTENING", "l1", "v1", "s1", null, 1800, MockTestSectionStatus.NOT_STARTED, now, now),
                new MockTestSection(UUID.randomUUID(), sessionId, 1, "READING", "r1", "v1", "s1", null, 3600, MockTestSectionStatus.NOT_STARTED, now, now)
        );
        return new MockTestSession(
                sessionId, UUID.randomUUID(), "mock-1", "v1", status,
                0, 10800, 0, null, null, null, now, now, sections
        );
    }

    @Test
    @DisplayName("Legal state progression: NOT_STARTED -> START -> IN_PROGRESS -> PAUSE -> RESUME -> SUBMIT")
    void legalProgression() {
        MockTestSession s0 = createSession(MockTestSessionStatus.NOT_STARTED);

        MockTestSession s1 = stateMachine.transition(s0, MockTestCommand.START);
        assertThat(s1.status()).isEqualTo(MockTestSessionStatus.IN_PROGRESS);

        MockTestSession s2 = stateMachine.transition(s1, MockTestCommand.PAUSE);
        assertThat(s2.status()).isEqualTo(MockTestSessionStatus.PAUSED);

        MockTestSession s3 = stateMachine.transition(s2, MockTestCommand.RESUME);
        assertThat(s3.status()).isEqualTo(MockTestSessionStatus.IN_PROGRESS);

        MockTestSession s4 = stateMachine.transition(s3, MockTestCommand.SUBMIT);
        assertThat(s4.status()).isEqualTo(MockTestSessionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("Next section and Prev section work within section bounds")
    void sectionTransitions() {
        MockTestSession s = createSession(MockTestSessionStatus.IN_PROGRESS);
        assertThat(s.currentSectionIndex()).isEqualTo(0);

        MockTestSession next = stateMachine.transition(s, MockTestCommand.NEXT_SECTION);
        assertThat(next.currentSectionIndex()).isEqualTo(1);

        assertThatThrownBy(() -> stateMachine.transition(next, MockTestCommand.NEXT_SECTION))
                .isInstanceOf(MockConflictException.class);

        MockTestSession prev = stateMachine.transition(next, MockTestCommand.PREV_SECTION);
        assertThat(prev.currentSectionIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("Terminal session rejects all mutation commands")
    void terminalSessionImmutability() {
        MockTestSession completed = createSession(MockTestSessionStatus.COMPLETED);
        assertThatThrownBy(() -> stateMachine.transition(completed, MockTestCommand.START))
                .isInstanceOf(MockConflictException.class);
        assertThatThrownBy(() -> stateMachine.transition(completed, MockTestCommand.SUBMIT))
                .isInstanceOf(MockConflictException.class);

        MockTestSession expired = createSession(MockTestSessionStatus.EXPIRED);
        assertThatThrownBy(() -> stateMachine.transition(expired, MockTestCommand.PAUSE))
                .isInstanceOf(MockConflictException.class);
    }
}
