package com.ieltsaitutor.tutor.adaptive;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.learning.intelligence.LearningIntelligenceService;
import com.ieltsaitutor.learning.intelligence.StudentLearningProfile;

/** Resolves only server-owned learning intelligence for the authenticated principal. */
@Service
public class AdaptiveTutorContextResolver {
    private final LearningIntelligenceService intelligence;

    public AdaptiveTutorContextResolver(LearningIntelligenceService intelligence) {
        this.intelligence = intelligence;
    }

    public AdaptiveTutorContext resolve(AuthPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            return new AdaptiveTutorContext(StudentLearningProfile.empty(null, Instant.now()), null, null, null);
        }
        var userId = principal.userId();
        return new AdaptiveTutorContext(intelligence.profile(userId), intelligence.skills(userId),
                intelligence.issues(userId), intelligence.roadmap(userId));
    }
}
