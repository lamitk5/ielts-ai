package com.ieltsaitutor.tutor.proactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.EvidenceState;
import com.ieltsaitutor.learning.intelligence.IssueKind;
import com.ieltsaitutor.learning.intelligence.IssueStatus;
import com.ieltsaitutor.learning.intelligence.LearningRoadmap;
import com.ieltsaitutor.learning.intelligence.RoadmapStatus;
import com.ieltsaitutor.learning.intelligence.StudentLearningIssue;
import com.ieltsaitutor.learning.intelligence.StudentLearningProfile;
import com.ieltsaitutor.preferences.UserPreferences;
import com.ieltsaitutor.tutor.adaptive.AdaptiveTutorContext;

class ProactiveTutorPolicyTest {
    private final UUID user = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-27T00:00:00Z");
    private final ProactiveTutorPolicy policy = new ProactiveTutorPolicy(java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));

    @Test
    void suggestsOneDismissibleActionOnlyForConfirmedEvidenceAndEnabledPreference() {
        var issue = issue(EvidenceState.CONFIRMED, now.minus(1, ChronoUnit.HOURS));
        var suggestion = policy.suggest(context(issue), new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                true, true, false, 40, 40, 1L, now), user,
                null, false, null).orElseThrow();
        assertThat(suggestion.issueId()).isEqualTo(issue.id());
        assertThat(suggestion.actionLabel()).isEqualTo("Luyện 10 câu mục tiêu");
    }

    @Test
    void respectsSessionLimitDismissalCooldownAndNewEvidenceGate() {
        var issue = issue(EvidenceState.CONFIRMED, now.minus(1, ChronoUnit.HOURS));
        var preferences = new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM", true, true, false, 40, 40, 1L, now);
        assertThat(policy.suggest(context(issue), preferences, user, null, true, null)).isEmpty();
        assertThat(policy.suggest(context(issue), preferences, user, now.minus(1, ChronoUnit.DAYS), false, null)).isEmpty();
        assertThat(policy.suggest(context(issue), preferences, user, null, false, issue.lastObservedAt())).isEmpty();
    }

    private AdaptiveTutorContext context(StudentLearningIssue issue) {
        return new AdaptiveTutorContext(StudentLearningProfile.empty(user, now), List.of(), List.of(issue),
                new LearningRoadmap(UUID.randomUUID(), user, RoadmapStatus.ACTIVE, 1, now, List.of(), now, now));
    }

    private StudentLearningIssue issue(EvidenceState state, Instant observedAt) {
        return new StudentLearningIssue(UUID.randomUUID(), user, IssueKind.WEAKNESS, null, "FALSE_NOT_GIVEN_CONFUSION",
                state, IssueStatus.OPEN, .9, 3, 2, observedAt, "CONFIRMED_RECURRENCE");
    }
}
