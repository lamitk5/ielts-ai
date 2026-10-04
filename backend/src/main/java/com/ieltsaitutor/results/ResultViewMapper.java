package com.ieltsaitutor.results;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.ieltsaitutor.assessment.objective.QuestionResult;
import com.ieltsaitutor.assessment.objective.QuestionResultView;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.SubmissionStatus;

public final class ResultViewMapper {
    private ResultViewMapper() {}

    public static LearnerResult objective(PracticeSubmission submission, List<QuestionResult> storedResults) {
        List<QuestionResultView> questions = storedResults == null ? List.of()
                : storedResults.stream().map(QuestionResultView::from).toList();
        boolean graded = submission.status() == SubmissionStatus.GRADED;
        int score = (int) questions.stream().filter(QuestionResultView::correct).count();
        Integer total = questions.isEmpty() ? null : questions.size();
        BigDecimal accuracy = total == null ? null
                : BigDecimal.valueOf(score * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        ResultStatus state = graded ? ResultStatus.READY
                : submission.status() == SubmissionStatus.FAILED ? ResultStatus.FAILED : ResultStatus.PROCESSING;
        return new LearnerResult(submission.id(), submission.skill(), submission.practiceId(), submission.practiceVersionId(),
                submission.status().name(), state, submission.submittedAt(), submission.durationSeconds(),
                graded && total != null ? score : null, graded ? total : null, graded ? accuracy : null,
                null, null, null, null, null, graded ? questions : List.of(), List.of(), null,
                List.of(ResultAction.ASK_TUTOR, ResultAction.SIMILAR_PRACTICE, ResultAction.VIEW_HISTORY),
                graded ? null : "Kết quả đang được hoàn tất.");
    }
}
