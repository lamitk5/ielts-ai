package com.ieltsaitutor.learning.intelligence;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LearningProfileService {
    private final LearningIntelligenceRepository repository;
    private final WeaknessStrengthAnalyzer issues;
    private final TrendAnalyzer trends;

    public LearningProfileService(LearningIntelligenceRepository repository, WeaknessStrengthAnalyzer issues, TrendAnalyzer trends) {
        this.repository = repository;
        this.issues = issues;
        this.trends = trends;
    }

    public StudentLearningProfile profile(UUID userId) {
        Instant now = Instant.now();
        List<LearningEvent> events = repository.findEvents(userId);
        if (events.isEmpty()) return StudentLearningProfile.empty(userId, now);
        long attempts = events.stream().filter(event -> event.eventType() == LearningEventType.PRACTICE_COMPLETED).count();
        long answers = events.stream().filter(event -> event.eventType() == LearningEventType.ANSWER_SUBMITTED
                || event.eventType() == LearningEventType.QUESTION_CORRECT || event.eventType() == LearningEventType.QUESTION_INCORRECT).count();
        return new StudentLearningProfile(userId, (int) attempts, (int) answers, 0,
                events.stream().map(LearningEvent::recordedAt).max(Instant::compareTo).orElse(null),
                answers == 0 ? EvidenceState.INSUFFICIENT_DATA : EvidenceState.OBSERVATION,
                answers < 20 ? "INSUFFICIENT_DATA" : "STABLE", answers == 0 ? 0d : Math.min(1d, answers / 20d), now, now);
    }

    public List<StudentSkillProfile> skills(UUID userId) {
        List<LearningEvent> events = repository.findEvents(userId);
        List<MistakeRecord> records = repository.findMistakes(userId);
        var issueList = issues.analyze(userId, records, Instant.now());
        return Arrays.stream(Skill.values()).map(skill -> {
            List<LearningEvent> skillEvents = events.stream().filter(event -> event.skill() == skill).toList();
            int answers = (int) skillEvents.stream().filter(event -> event.eventType() == LearningEventType.QUESTION_CORRECT
                    || event.eventType() == LearningEventType.QUESTION_INCORRECT).count();
            int correct = (int) skillEvents.stream().filter(event -> event.eventType() == LearningEventType.QUESTION_CORRECT).count();
            var trend = trends.analyze(userId, skill, skillEvents);
            int weaknessCount = (int) issueList.stream().filter(issue -> issue.skill() == skill && issue.kind() == IssueKind.WEAKNESS).count();
            return new StudentSkillProfile(userId, skill, (int) skillEvents.stream().filter(e -> e.eventType() == LearningEventType.PRACTICE_COMPLETED).count(),
                    answers, answers == 0 ? 0d : correct / (double) answers, null,
                    answers == 0 ? EvidenceState.INSUFFICIENT_DATA : EvidenceState.OBSERVATION,
                    trend.state().name(), 0, weaknessCount, skillEvents.stream().map(LearningEvent::recordedAt).max(Instant::compareTo).orElse(null), Instant.now());
        }).toList();
    }
}
