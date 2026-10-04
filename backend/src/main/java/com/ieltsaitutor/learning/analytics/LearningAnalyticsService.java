package com.ieltsaitutor.learning.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ieltsaitutor.learning.intelligence.LearningEvent;
import com.ieltsaitutor.learning.intelligence.LearningEventType;
import com.ieltsaitutor.learning.intelligence.MistakeRecord;
import com.ieltsaitutor.learning.intelligence.StudentSkillProfile;

public class LearningAnalyticsService {
    public LearningAnalytics build(UUID userId, List<LearningEvent> events, List<MistakeRecord> mistakes,
            List<StudentSkillProfile> skillProfiles) {
        List<SkillAnalytics> skills = (skillProfiles == null ? List.<StudentSkillProfile>of() : skillProfiles).stream()
                .map(item -> new SkillAnalytics(item.skill().name(), item.attemptCount(), item.evaluatedItemCount(),
                        item.evaluatedItemCount() == 0 ? null : item.accuracyRate() * 100.0, item.latestBandEstimate(), item.evidenceState().name()))
                .toList();
        Map<String, int[]> grouped = new HashMap<>();
        for (LearningEvent event : events == null ? List.<LearningEvent>of() : events) {
            if (event.eventType() != LearningEventType.QUESTION_CORRECT && event.eventType() != LearningEventType.QUESTION_INCORRECT) continue;
            Object raw = event.payload().get("questionType");
            if (raw == null) continue;
            int[] tally = grouped.computeIfAbsent(raw.toString(), ignored -> new int[2]);
            tally[1]++;
            if (event.eventType() == LearningEventType.QUESTION_CORRECT) tally[0]++;
        }
        List<QuestionTypeAnalytics> questionTypes = grouped.entrySet().stream()
                .map(entry -> new QuestionTypeAnalytics(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
                        entry.getValue()[1] == 0 ? 0d : entry.getValue()[0] * 100d / entry.getValue()[1]))
                .sorted(Comparator.comparingDouble(QuestionTypeAnalytics::accuracyPercent)).toList();
        String weakest = questionTypes.isEmpty() ? null : questionTypes.get(0).questionType();
        String strongest = questionTypes.isEmpty() ? null : questionTypes.get(questionTypes.size() - 1).questionType();
        return new LearningAnalytics(skills, questionTypes, strongest, weakest, !questionTypes.isEmpty() || !skills.isEmpty());
    }
}
