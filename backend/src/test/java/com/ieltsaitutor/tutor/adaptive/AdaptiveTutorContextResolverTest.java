package com.ieltsaitutor.tutor.adaptive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.auth.AuthPrincipal;
import com.ieltsaitutor.auth.UserRole;
import com.ieltsaitutor.learning.intelligence.EvidenceState;
import com.ieltsaitutor.learning.intelligence.LearningIntelligenceService;
import com.ieltsaitutor.learning.intelligence.StudentLearningIssue;
import com.ieltsaitutor.learning.intelligence.StudentLearningProfile;
import com.ieltsaitutor.learning.intelligence.StudentSkillProfile;
import com.ieltsaitutor.learning.intelligence.Skill;

class AdaptiveTutorContextResolverTest {
    @Test
    void resolvesOnlyTheAuthenticatedUserProfile() {
        UUID user = UUID.randomUUID();
        LearningIntelligenceService service = mock(LearningIntelligenceService.class);
        when(service.profile(user)).thenReturn(StudentLearningProfile.empty(user, Instant.now()));
        when(service.skills(user)).thenReturn(List.of(StudentSkillProfile.empty(user, Skill.READING, Instant.now())));
        when(service.issues(user)).thenReturn(List.of());
        when(service.roadmap(user)).thenReturn(new com.ieltsaitutor.learning.intelligence.LearningRoadmap(UUID.randomUUID(), user,
                com.ieltsaitutor.learning.intelligence.RoadmapStatus.COMPLETED, 1, Instant.now(), List.of(), Instant.now(), Instant.now()));

        AdaptiveTutorContext context = new AdaptiveTutorContextResolver(service).resolve(
                new AuthPrincipal(user, "student@example.com", "Mai", UserRole.CUSTOMER));

        assertEquals(user, context.profile().userId());
        assertEquals(1, context.skills().size());
        assertTrue(context.issues().isEmpty());
    }

    @Test
    void anonymousContextHasNoPersonalizedData() {
        AdaptiveTutorContext context = new AdaptiveTutorContextResolver(mock(LearningIntelligenceService.class)).resolve(null);
        assertTrue(context.profile().evidenceState() == EvidenceState.INSUFFICIENT_DATA);
        assertTrue(context.skills().isEmpty());
    }
}
