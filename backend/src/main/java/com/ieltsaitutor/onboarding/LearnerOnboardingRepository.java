package com.ieltsaitutor.onboarding;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence boundary for learner onboarding goals. Every method is scoped by
 * {@code userId}; there is deliberately no method able to write adaptive
 * evidence, keeping self-report structurally separated from measured weakness.
 */
public interface LearnerOnboardingRepository {

    LearnerOnboardingProfile find(UUID userId);

    void insertDefault(UUID userId, Instant now);

    boolean update(UUID userId, LearnerOnboardingProfile profile, long expectedVersion);
}