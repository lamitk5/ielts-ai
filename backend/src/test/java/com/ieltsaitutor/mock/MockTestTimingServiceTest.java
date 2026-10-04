package com.ieltsaitutor.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockTestTimingServiceTest {

    private final Instant baseTime = Instant.parse("2026-10-02T10:00:00Z");

    private MockTestSession createTimedSession(int limitSeconds, int elapsed) {
        return new MockTestSession(
                UUID.randomUUID(), UUID.randomUUID(), "mock-1", "v1", MockTestSessionStatus.IN_PROGRESS,
                0, limitSeconds, elapsed, baseTime, baseTime.plusSeconds(limitSeconds), null, baseTime, baseTime, List.of()
        );
    }

    @Test
    @DisplayName("Calculates remaining and elapsed seconds accurately using fixed clock")
    void timingCalculations() {
        // Clock is 30 minutes after start
        Clock clock = Clock.fixed(baseTime.plusSeconds(1800), ZoneOffset.UTC);
        MockTestTimingService timing = new MockTestTimingService(clock);

        MockTestSession session = createTimedSession(10800, 0); // 3 hours limit
        assertThat(timing.calculateElapsedSeconds(session)).isEqualTo(1800);
        assertThat(timing.calculateRemainingSeconds(session)).isEqualTo(9000);
        assertThat(timing.isExpired(session)).isFalse();
    }

    @Test
    @DisplayName("Detects expired session when clock exceeds expiration time")
    void detectsExpiry() {
        // Clock is 3 hours and 1 minute after start
        Clock clock = Clock.fixed(baseTime.plusSeconds(10860), ZoneOffset.UTC);
        MockTestTimingService timing = new MockTestTimingService(clock);

        MockTestSession session = createTimedSession(10800, 0);
        assertThat(timing.isExpired(session)).isTrue();
        assertThat(timing.calculateRemainingSeconds(session)).isEqualTo(0);
    }
}
