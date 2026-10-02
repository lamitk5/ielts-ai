package com.ieltsaitutor.assessment.objective;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ieltsaitutor.submission.PracticeSubmission;

public record ObjectiveResultView(UUID id, String skill, String practiceId, String practiceVersionId,
        String publishedSetId, int publicationRevision, String status, Instant startedAt, Instant submittedAt,
        long durationSeconds, Integer score, Integer total, BigDecimal accuracy, String scoringPolicyVersion,
        List<QuestionResultView> questionResults, List<String> actions) {
    public static ObjectiveResultView from(PracticeSubmission submission, List<QuestionResult> results) {
        List<QuestionResultView> views = results == null ? List.of() : results.stream().map(QuestionResultView::from).toList();
        int score = (int) views.stream().filter(QuestionResultView::correct).count();
        Integer total = views.isEmpty() ? null : views.size();
        BigDecimal accuracy = total == null ? null : BigDecimal.valueOf(score * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP);
        String policy = results == null || results.isEmpty() ? null : results.getFirst().scoringPolicyVersion();
        return new ObjectiveResultView(submission.id(), submission.skill(), submission.practiceId(), submission.practiceVersionId(),
                submission.publishedSetId(), submission.publicationRevision(), submission.status().name(), submission.startedAt(),
                submission.submittedAt(), submission.durationSeconds(), total == null ? null : score, total, accuracy, policy,
                views, List.of("review-mistakes", "ask-tutor", "similar-practice"));
    }
}
