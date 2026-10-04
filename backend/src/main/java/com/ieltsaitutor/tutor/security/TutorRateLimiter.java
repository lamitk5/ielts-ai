package com.ieltsaitutor.tutor.security;

import com.ieltsaitutor.auth.AuthPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

@Component
public class TutorRateLimiter {
    private final int guestLimit;
    private final int memberLimit;
    private final Duration window;
    private final Map<String, ArrayDeque<Long>> requests = new HashMap<>();

    public TutorRateLimiter(
            @Value("$" + "{ai.tutor.guest-rate-limit:10}") int guestLimit,
            @Value("$" + "{ai.tutor.member-rate-limit:30}") int memberLimit,
            @Value("$" + "{ai.tutor.rate-window:60s}") Duration window) {
        if (guestLimit < 1 || memberLimit < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Tutor rate limit must be positive.");
        }
        this.guestLimit = guestLimit;
        this.memberLimit = memberLimit;
        this.window = window;
    }

    public synchronized TutorRateLimitDecision allow(AuthPrincipal principal) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        String bucket = principal == null ? "guest" : "member:" + principal.userId();
        int limit = principal == null ? guestLimit : memberLimit;
        ArrayDeque<Long> timestamps = requests.computeIfAbsent(bucket, ignored -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) timestamps.removeFirst();
        if (timestamps.size() >= limit) {
            long retryAfter = timestamps.isEmpty() ? window.toSeconds()
                    : Math.max(1, (timestamps.peekFirst() + window.toMillis() - now + 999) / 1000);
            return new TutorRateLimitDecision(false, 0, retryAfter);
        }
        timestamps.addLast(now);
        return new TutorRateLimitDecision(true, limit - timestamps.size(), 0);
    }
}
