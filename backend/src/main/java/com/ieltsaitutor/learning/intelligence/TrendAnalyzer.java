package com.ieltsaitutor.learning.intelligence;

import java.util.List;
import java.util.UUID;

public class TrendAnalyzer {
    public TrendResult analyze(UUID userId, Skill skill, List<LearningEvent> events) {
        List<LearningEvent> comparable = events == null ? List.of() : events.stream()
                .filter(event -> event.userId().equals(userId) && event.skill() == skill
                        && (event.eventType() == LearningEventType.QUESTION_CORRECT || event.eventType() == LearningEventType.QUESTION_INCORRECT))
                .toList();
        if (comparable.size() < 6) return new TrendResult(EvidenceState.INSUFFICIENT_DATA, comparable.size(), 0d);
        int split = comparable.size() / 2;
        double first = comparable.subList(0, split).stream().filter(e -> e.eventType() == LearningEventType.QUESTION_CORRECT).count() / (double) split;
        double second = comparable.subList(split, comparable.size()).stream().filter(e -> e.eventType() == LearningEventType.QUESTION_CORRECT).count()
                / (double) (comparable.size() - split);
        double delta = second - first;
        EvidenceState state = delta >= .15 ? EvidenceState.IMPROVING : delta <= -.15 ? EvidenceState.DECLINING : EvidenceState.STABLE;
        return new TrendResult(state, comparable.size(), delta);
    }

    public record TrendResult(EvidenceState state, int sampleSize, double delta) {}
}
