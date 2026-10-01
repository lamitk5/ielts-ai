package com.ieltsaitutor.tutor.security;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TutorQuotaPolicyTest {
    @Test
    void guestAndMemberBudgetsAreBoundedIndependently() {
        TutorRateLimiter limiter = new TutorRateLimiter(1, 2, Duration.ofMinutes(1));
        AuthPrincipal member = new AuthPrincipal(UUID.randomUUID(), "member@test", "Member", UserRole.CUSTOMER);

        assertThat(limiter.allow(null).allowed()).isTrue();
        assertThat(limiter.allow(null).allowed()).isFalse();
        assertThat(limiter.allow(member).allowed()).isTrue();
        assertThat(limiter.allow(member).allowed()).isTrue();
        assertThat(limiter.allow(member).allowed()).isFalse();
    }

    @Test
    void decisionDoesNotExposeIdentityOrPromptContent() {
        TutorRateLimitDecision decision = new TutorRateLimitDecision(false, 0, 60);

        assertThat(decision.toString()).doesNotContain("member@test", "prompt", "answer");
    }
}
