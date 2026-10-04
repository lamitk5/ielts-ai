package com.ieltsaitutor.results;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ieltsaitutor.assessment.objective.QuestionResult;
import com.ieltsaitutor.assessment.objective.QuestionResultRepository;
import com.ieltsaitutor.speaking.SpeakingReviewRepository;
import com.ieltsaitutor.speaking.SpeakingSubmissionRepository;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionConflictException;
import com.ieltsaitutor.submission.SubmissionStatus;
import com.ieltsaitutor.writing.WritingEvaluationRepository;
import com.ieltsaitutor.writing.WritingEvaluationResult;
import com.ieltsaitutor.writing.WritingSubmissionVersion;
import com.ieltsaitutor.writing.WritingVersionRepository;

@Service
public class LearnerResultService {
    private final PracticeSubmissionRepository submissions;
    private final QuestionResultRepository objectiveResults;
    private final WritingVersionRepository writingVersions;
    private final WritingEvaluationRepository writingEvaluations;
    private final SpeakingSubmissionRepository speakingSubmissions;
    private final SpeakingReviewRepository speakingReviews;
    private final SubmissionReviewRepository humanReviews;

    @Autowired
    public LearnerResultService(PracticeSubmissionRepository submissions, QuestionResultRepository objectiveResults,
            WritingVersionRepository writingVersions, WritingEvaluationRepository writingEvaluations,
            SpeakingSubmissionRepository speakingSubmissions, SpeakingReviewRepository speakingReviews,
            SubmissionReviewRepository humanReviews) {
        this.submissions = submissions;
        this.objectiveResults = objectiveResults;
        this.writingVersions = writingVersions;
        this.writingEvaluations = writingEvaluations;
        this.speakingSubmissions = speakingSubmissions;
        this.speakingReviews = speakingReviews;
        this.humanReviews = humanReviews;
    }

    public LearnerResultService(PracticeSubmissionRepository submissions, QuestionResultRepository objectiveResults) {
        this(submissions, objectiveResults, null, null, null, null, null);
    }

    public LearnerResult get(UUID ownerId, UUID submissionId) {
        PracticeSubmission submission = submissions.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission not found or access denied"));
        return build(submission);
    }

    public LearnerResult getForAdmin(UUID submissionId) {
        PracticeSubmission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission not found"));
        return build(submission);
    }

    private LearnerResult build(PracticeSubmission submission) {
        String skill = submission.skill();
        if ("READING".equals(skill) || "LISTENING".equals(skill)) {
            List<QuestionResult> stored = submission.status() == SubmissionStatus.GRADED && objectiveResults != null
                    ? objectiveResults.findByOwnedSubmission(submission.userId(), submission.id()) : List.of();
            return ResultViewMapper.objective(submission, stored);
        }

        LearnerResult base = new LearnerResult(submission.id(), skill, submission.practiceId(), submission.practiceVersionId(),
                submission.status().name(), statusFor(submission), submission.submittedAt(), submission.durationSeconds(),
                null, null, null, null, null, null, null, null, List.of(), List.of(), null,
                List.of(ResultAction.ASK_TUTOR, ResultAction.VIEW_HISTORY), null);
        if ("WRITING".equals(skill) && writingVersions != null && writingEvaluations != null) return writing(base, submission);
        if ("SPEAKING".equals(skill) && speakingSubmissions != null) return speaking(base, submission);
        return base;
    }

    private LearnerResult writing(LearnerResult base, PracticeSubmission submission) {
        List<WritingSubmissionVersion> versions = writingVersions.findBySubmissionId(submission.id());
        WritingSubmissionVersion latest = versions.stream().findFirst().orElse(null);
        WritingEvaluationResult evaluation = latest == null ? null : writingEvaluations.findLatestByVersionId(latest.id()).orElse(null);
        List<LearnerResult.VersionSummary> summaries = versions.stream().map(v -> {
            String status = writingEvaluations.findLatestByVersionId(v.id()).map(WritingEvaluationResult::status).orElse("UNAVAILABLE");
            return new LearnerResult.VersionSummary(v.id(), v.versionNumber(), v.wordCount(), v.createdAt(), status);
        }).toList();
        LearnerResult.EvaluationSummary ai = evaluation == null ? null : evaluation(evaluation);
        ResultStatus status = evaluation == null ? ResultStatus.PROCESSING
                : "GRADED".equals(evaluation.status()) ? ResultStatus.READY
                : "FAILED".equals(evaluation.status()) ? ResultStatus.FAILED : ResultStatus.PARTIALLY_AVAILABLE;
        LearnerResult.HumanReviewSummary human = humanReview(submission.id());
        return copy(base, status, evaluation == null ? null : evaluation.overallBandEstimate(), evaluation == null ? null : evaluation.disclaimer(),
                ai, human, summaries, null, evaluation == null ? "Chưa có đánh giá AI cho phiên bản này." : null,
                List.of(ResultAction.ASK_TUTOR, ResultAction.RETRY, ResultAction.VIEW_HISTORY));
    }

    private LearnerResult speaking(LearnerResult base, PracticeSubmission submission) {
        var speaking = speakingSubmissions.findBySubmissionId(submission.id()).orElse(null);
        var review = speakingReviews == null ? null : speakingReviews.findLatestBySubmission(submission.id()).orElse(null);
        LearnerResult.SpeakingSummary summary = speaking == null ? null : new LearnerResult.SpeakingSummary(speaking.id(), speaking.promptId(),
                speaking.status().name(), speaking.transcript(), speaking.transcriptSource(), speaking.audioStorageKey() != null);
        LearnerResult.HumanReviewSummary human = review == null ? humanReview(submission.id())
                : new LearnerResult.HumanReviewSummary(review.id(), review.reviewVersion(), review.overallBand(), review.fluencyCoherence(),
                        review.lexicalResource(), review.grammaticalRange(), review.pronunciation(), review.reviewerFeedback(), review.status(),
                        review.reviewerUserId(), review.createdAt());
        ResultStatus status = human == null ? ResultStatus.PARTIALLY_AVAILABLE : ResultStatus.READY;
        return copy(base, status, null, null, null, human, List.of(), summary,
                human == null ? "Bài nói đang chờ đánh giá thủ công." : null,
                List.of(ResultAction.ASK_TUTOR, ResultAction.VIEW_HISTORY));
    }

    private LearnerResult.HumanReviewSummary humanReview(UUID submissionId) {
        if (humanReviews == null) return null;
        return humanReviews.findLatestBySubmission(submissionId).map(review -> new LearnerResult.HumanReviewSummary(review.id(),
                review.reviewVersion(), review.overallBand(), review.fluencyCoherence(), review.lexicalResource(), review.grammaticalRange(),
                review.pronunciation(), review.reviewerFeedback(), review.status(), review.reviewerUserId(), review.createdAt())).orElse(null);
    }

    private LearnerResult.EvaluationSummary evaluation(WritingEvaluationResult item) {
        return new LearnerResult.EvaluationSummary(item.id(), item.evaluationVersion(), item.overallBandEstimate(), item.criteria(),
                item.strengths(), item.issues(), item.suggestions(), item.priorityImprovements(), item.groundingStatus(), item.status(),
                item.disclaimer(), item.createdAt());
    }

    private LearnerResult copy(LearnerResult base, ResultStatus resultStatus, Double band, String disclaimer,
            LearnerResult.EvaluationSummary evaluation, LearnerResult.HumanReviewSummary human,
            List<LearnerResult.VersionSummary> versions, LearnerResult.SpeakingSummary speaking, String message,
            List<ResultAction> actions) {
        return new LearnerResult(base.submissionId(), base.skill(), base.practiceId(), base.practiceVersionId(), base.status(), resultStatus,
                base.submittedAt(), base.durationSeconds(), base.score(), base.total(), base.accuracy(), band, null, disclaimer,
                evaluation, human, base.questionResults(), versions, speaking, actions, message);
    }

    private ResultStatus statusFor(PracticeSubmission submission) {
        if (submission.status() == SubmissionStatus.FAILED) return ResultStatus.FAILED;
        if (submission.status() == SubmissionStatus.GRADED) return ResultStatus.READY;
        return ResultStatus.PROCESSING;
    }
}
