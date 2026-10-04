package com.ieltsaitutor.tutor.proactive;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.EvidenceState;
import com.ieltsaitutor.learning.intelligence.IssueStatus;
import com.ieltsaitutor.learning.intelligence.RoadmapStatus;
import com.ieltsaitutor.learning.intelligence.StudentLearningIssue;
import com.ieltsaitutor.preferences.UserPreferences;
import com.ieltsaitutor.tutor.adaptive.AdaptiveTutorContext;

/** Evidence-gated, dismissible policy; it does not create a learning event. */
public final class ProactiveTutorPolicy {
    private static final Duration DISMISSAL_COOLDOWN = Duration.ofDays(7);
    private final Clock clock;

    public ProactiveTutorPolicy() { this(Clock.systemUTC()); }
    public ProactiveTutorPolicy(Clock clock) { this.clock = clock; }

    public Optional<ProactiveTutorSuggestion> suggest(AdaptiveTutorContext context, UserPreferences preferences,
            UUID userId, Instant dismissedAt, boolean alreadySuggestedThisSession, Instant evidenceSeenAt) {
        if (context == null || preferences == null || !Boolean.TRUE.equals(preferences.proactiveAiEnabled())
                || userId == null || alreadySuggestedThisSession || context.profile() == null
                || !userId.equals(context.profile().userId()) || context.roadmap() == null
                || !userId.equals(context.roadmap().userId()) || context.roadmap().status() != RoadmapStatus.ACTIVE) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        if (dismissedAt != null && dismissedAt.plus(DISMISSAL_COOLDOWN).isAfter(now)) return Optional.empty();
        return context.issues().stream().filter(issue -> eligible(issue, userId, evidenceSeenAt))
                .findFirst().map(issue -> new ProactiveTutorSuggestion(UUID.randomUUID(), userId, issue.id(), issue.skill(),
                        "Ôn lại " + (issue.category() == null ? "điểm cần cải thiện" : issue.category()),
                        "Luyện 10 câu mục tiêu", issue.evidenceCode(), now));
    }

    private boolean eligible(StudentLearningIssue issue, UUID userId, Instant evidenceSeenAt) {
        return issue != null && userId.equals(issue.userId()) && issue.status() == IssueStatus.OPEN
                && issue.evidenceState() == EvidenceState.CONFIRMED
                && issue.lastObservedAt() != null
                && (evidenceSeenAt == null || issue.lastObservedAt().isAfter(evidenceSeenAt));
    }
}
