package com.ieltsaitutor.learning.plan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.*;
import com.ieltsaitutor.onboarding.LearnerOnboardingRepository;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;

class TodaysPlanServiceTest {
    @Test void oneObservationDoesNotBecomeTargetedPlan() {
        UUID user = UUID.randomUUID();
        LearningIntelligenceService intelligence = mock(LearningIntelligenceService.class);
        when(intelligence.issues(user)).thenReturn(List.of(new StudentLearningIssue(UUID.randomUUID(), user, IssueKind.WEAKNESS,
                Skill.READING, "DETAIL", EvidenceState.OBSERVATION, IssueStatus.OPEN, .4, 1, 1, Instant.now(), "OBS")));
        TodaysPlanService service = new TodaysPlanService(intelligence, null, SyntheticPracticeCatalog.inMemory());
        TodaysPlanService.TodaysPlanResponse response = service.plan(user, 30);
        assertTrue(response.items().stream().noneMatch(item -> item.reasonCode() == TodaysPlanReason.RECURRING_MISTAKE));
        assertTrue(response.items().size() <= 5);
    }

    @Test void repeatedConfirmedMistakesAreBoundedAndExplainable() {
        UUID user = UUID.randomUUID();
        LearningIntelligenceService intelligence = mock(LearningIntelligenceService.class);
        when(intelligence.issues(user)).thenReturn(java.util.stream.IntStream.range(0, 8).mapToObj(i ->
                new StudentLearningIssue(UUID.randomUUID(), user, IssueKind.WEAKNESS, Skill.READING, "DETAIL" + i,
                        EvidenceState.CONFIRMED, IssueStatus.OPEN, .8, 2, 2, Instant.now(), "ISSUE" + i)).toList());
        TodaysPlanService service = new TodaysPlanService(intelligence, null, SyntheticPracticeCatalog.inMemory());
        TodaysPlanService.TodaysPlanResponse response = service.plan(user, 45);
        assertTrue(response.items().size() <= 5);
        assertTrue(response.items().stream().allMatch(item -> !item.reason().isBlank()));
    }
}
