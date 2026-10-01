package com.ieltsaitutor.tutor.proactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.EvidenceState;
import com.ieltsaitutor.learning.intelligence.IssueKind;
import com.ieltsaitutor.learning.intelligence.IssueStatus;
import com.ieltsaitutor.learning.intelligence.StudentLearningIssue;
import com.ieltsaitutor.learning.intelligence.StudentLearningProfile;
import com.ieltsaitutor.preferences.UserPreferences;
import com.ieltsaitutor.tutor.adaptive.AdaptiveTutorContext;

class ProactiveTutorSecurityTest {
    @Test
    void foreignOrInsufficientEvidenceCannotProduceSuggestion() {
        UUID owner = UUID.randomUUID();
        UUID caller = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-27T00:00:00Z");
        var foreignIssue = new StudentLearningIssue(UUID.randomUUID(), owner, IssueKind.WEAKNESS, null, "grammar",
                EvidenceState.CONFIRMED, IssueStatus.OPEN, .9, 3, 2, now, "CONFIRMED");
        var foreignContext = new AdaptiveTutorContext(StudentLearningProfile.empty(owner, now), List.of(), List.of(foreignIssue), null);
        var policy = new ProactiveTutorPolicy(java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));

        assertThat(policy.suggest(foreignContext, new UserPreferences("SYSTEM", "GOLD", "DEFAULT", "DEFAULT", "SYSTEM",
                true, true, false, 40, 40, 1L, now), caller, null, false, null)).isEmpty();
        var insufficient = new StudentLearningIssue(UUID.randomUUID(), caller, IssueKind.WEAKNESS, null, "grammar",
                EvidenceState.INSUFFICIENT_DATA, IssueStatus.OPEN, 0, 0, 0, now, "NONE");
        assertThat(policy.suggest(new AdaptiveTutorContext(StudentLearningProfile.empty(caller, now), List.of(), List.of(insufficient), null),
                UserPreferences.defaults(now), caller, null, false, null)).isEmpty();
    }
}
