package com.ieltsaitutor.mock;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
public class MockTestTimingService {

    private final Clock clock;

    public MockTestTimingService() {
        this(Clock.systemUTC());
    }

    public MockTestTimingService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public MockTestSession initializeTiming(MockTestSession session) {
        Instant now = clock.instant();
        Instant expires = now.plusSeconds(session.totalTimeLimitSeconds());
        return new MockTestSession(
                session.id(),
                session.userId(),
                session.mockTestId(),
                session.mockTestVersion(),
                session.status(),
                session.currentSectionIndex(),
                session.totalTimeLimitSeconds(),
                0,
                now,
                expires,
                null,
                session.createdAt(),
                now,
                session.sections()
        );
    }

    public boolean isExpired(MockTestSession session) {
        if (session.expiresAt() == null) return false;
        return clock.instant().isAfter(session.expiresAt());
    }

    public int calculateRemainingSeconds(MockTestSession session) {
        if (session.expiresAt() == null) return session.totalTimeLimitSeconds();
        Instant now = clock.instant();
        long diff = Duration.between(now, session.expiresAt()).getSeconds();
        return (int) Math.max(0, diff);
    }

    public int calculateElapsedSeconds(MockTestSession session) {
        if (session.startedAt() == null) return session.elapsedSeconds();
        Instant now = clock.instant();
        long diff = Duration.between(session.startedAt(), now).getSeconds();
        return (int) Math.min(session.totalTimeLimitSeconds(), Math.max(session.elapsedSeconds(), diff));
    }
}
