package com.ieltsaitutor.learning.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.learning.intelligence.EvidenceState;
import com.ieltsaitutor.learning.intelligence.LearningIntelligenceService;
import com.ieltsaitutor.learning.intelligence.Skill;
import com.ieltsaitutor.learning.intelligence.StudentLearningIssue;
import com.ieltsaitutor.onboarding.LearnerOnboardingProfile;
import com.ieltsaitutor.onboarding.LearnerOnboardingRepository;
import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;

@Service
public class TodaysPlanService {
    private static final int MAX_ITEMS = 5;
    private final LearningIntelligenceService intelligence;
    private final LearnerOnboardingRepository onboarding;
    private final SyntheticPracticeCatalog catalog;

    public TodaysPlanService(LearningIntelligenceService intelligence, LearnerOnboardingRepository onboarding,
            SyntheticPracticeCatalog catalog) {
        this.intelligence = intelligence;
        this.onboarding = onboarding;
        this.catalog = catalog;
    }

    public TodaysPlanResponse plan(UUID userId, Integer requestedMinutes) {
        if (userId == null) throw new IllegalArgumentException("owner is required");
        int budget = boundedMinutes(requestedMinutes, onboarding == null ? null : onboarding.find(userId));
        List<TodaysPlanItem> items = new ArrayList<>();
        intelligence.issues(userId).stream()
                .filter(this::targetable)
                .sorted(Comparator.comparingInt(StudentLearningIssue::occurrenceCount).reversed()
                        .thenComparing(i -> i.skill().name()).thenComparing(StudentLearningIssue::category))
                .limit(MAX_ITEMS)
                .forEach(issue -> addIfFits(items, target(issue), budget));

        if (items.isEmpty()) {
            addFallback(items, budget, onboarding == null ? null : onboarding.find(userId));
        }
        return new TodaysPlanResponse(items, budget, items.isEmpty(),
                items.isEmpty() ? "Chưa có đủ dữ liệu để tạo gợi ý cá nhân hóa. Hãy bắt đầu một bài luyện phù hợp." : null);
    }

    private boolean targetable(StudentLearningIssue issue) {
        return issue != null && issue.occurrenceCount() >= 2
                && (issue.evidenceState() == EvidenceState.CONFIRMED || issue.evidenceState() == EvidenceState.EMERGING);
    }

    private TodaysPlanItem target(StudentLearningIssue issue) {
        PracticeSet set = practice(issue.skill());
        String practiceId = set == null ? null : set.id();
        String title = set == null ? "Luyện " + label(issue.skill()) : set.title();
        String route = set == null ? "/practice/" + issue.skill().name().toLowerCase() : "/practice/" + issue.skill().name().toLowerCase() + "/" + set.id();
        return new TodaysPlanItem(UUID.randomUUID(), issue.skill(), practiceId, title, 15,
                TodaysPlanReason.RECURRING_MISTAKE,
                "Ôn lại " + issue.category() + " vì đã xuất hiện " + issue.occurrenceCount() + " lần trong dữ liệu luyện tập.",
                issue.evidenceCode() == null ? "issue:" + issue.id() : issue.evidenceCode(), true, route);
    }

    private void addFallback(List<TodaysPlanItem> items, int budget, LearnerOnboardingProfile profile) {
        Skill skill = profile == null ? Skill.READING : profile.perceivedWeakestSkill();
        if (skill == null) skill = Skill.READING;
        PracticeSet set = practice(skill);
        String title = set == null ? "Bắt đầu luyện " + label(skill) : set.title();
        String route = set == null ? "/practice/" + skill.name().toLowerCase() : "/practice/" + skill.name().toLowerCase() + "/" + set.id();
        TodaysPlanReason code = profile != null && profile.isGoalEstablished() ? TodaysPlanReason.ONBOARDING_GOAL : TodaysPlanReason.DIAGNOSTIC_FALLBACK;
        String reason = code == TodaysPlanReason.ONBOARDING_GOAL
                ? "Bắt đầu theo mục tiêu học tập bạn đã khai báo; đây chưa phải là kết luận năng lực."
                : "Chưa đủ dữ liệu đo lường để cá nhân hóa; bắt đầu bằng một bài luyện nền tảng.";
        addIfFits(items, new TodaysPlanItem(UUID.randomUUID(), skill, set == null ? null : set.id(), title,
                Math.min(15, budget), code, reason, code.name().toLowerCase(), false, route), budget);
    }

    private void addIfFits(List<TodaysPlanItem> items, TodaysPlanItem item, int budget) {
        int used = items.stream().mapToInt(TodaysPlanItem::durationMinutes).sum();
        if (items.size() < MAX_ITEMS && used + item.durationMinutes() <= budget) items.add(item);
    }

    private PracticeSet practice(Skill skill) {
        if (catalog == null || (skill != Skill.READING && skill != Skill.LISTENING)) return null;
        return catalog.sets(skill.name()).stream().findFirst().orElse(null);
    }

    private int boundedMinutes(Integer requested, LearnerOnboardingProfile profile) {
        int value = requested != null ? requested : profile != null && profile.dailyStudyMinutes() != null ? profile.dailyStudyMinutes() : 30;
        return Math.max(5, Math.min(240, value));
    }

    private String label(Skill skill) { return skill.name().substring(0, 1) + skill.name().substring(1).toLowerCase(); }

    public record TodaysPlanResponse(List<TodaysPlanItem> items, int availableMinutes, boolean insufficientEvidence, String message) {
        public TodaysPlanResponse { items = items == null ? List.of() : List.copyOf(items); }
    }
}
