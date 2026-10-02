package com.ieltsaitutor.assessment.objective;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionAnswerRepository;
import com.ieltsaitutor.submission.SubmissionAnswerSnapshot;
import com.ieltsaitutor.submission.SubmissionConflictException;
import com.ieltsaitutor.submission.SubmissionStatus;

@Service
public class ObjectiveSubmissionScoringService {
    private final PracticeSubmissionRepository submissions;
    private final SubmissionAnswerRepository answers;
    private final QuestionResultRepository results;
    private final SyntheticPracticeCatalog catalog;
    private final DeterministicObjectiveScorer scorer;

    public ObjectiveSubmissionScoringService(PracticeSubmissionRepository submissions, SubmissionAnswerRepository answers,
            QuestionResultRepository results, SyntheticPracticeCatalog catalog, DeterministicObjectiveScorer scorer) {
        this.submissions = submissions;
        this.answers = answers;
        this.results = results;
        this.catalog = catalog;
        this.scorer = scorer;
    }

    @Transactional
    public ObjectiveSubmissionResult score(UUID ownerId, UUID submissionId) {
        PracticeSubmission submission = submissions.findByOwnerAndId(ownerId, submissionId)
                .orElseThrow(() -> new SubmissionConflictException("Submission is not available"));
        if (!isObjective(submission.skill())) throw new SubmissionConflictException("Objective scoring is unavailable for this skill");
        List<QuestionResult> existing = results.findByOwnedSubmission(ownerId, submissionId);
        if (submission.status() == SubmissionStatus.GRADED && !existing.isEmpty()) {
            return new ObjectiveSubmissionResult(submission, summarize(existing));
        }
        if (submission.status() != SubmissionStatus.SUBMITTED && submission.status() != SubmissionStatus.SCORING) {
            throw new SubmissionConflictException("Submission is not ready for scoring");
        }
        SubmissionAnswerSnapshot snapshot = answers.findBySubmissionId(submissionId)
                .filter(item -> item.userId().equals(ownerId))
                .orElseThrow(() -> new SubmissionConflictException("Submitted answers are not available"));
        PracticeSet practice = catalog.find(submission.skill().toLowerCase(), submission.publishedSetId());
        submissions.updateStatus(submissionId, SubmissionStatus.SCORING, Instant.now());
        ObjectiveScore score = scorer.score(practice.questions(), snapshot.answers());
        List<QuestionResult> questionResults = score.questionResults().stream().map(item -> new QuestionResult(
                UUID.randomUUID(), submissionId, ownerId, item.questionId(), item.questionType(), item.learnerAnswer(),
                item.normalizedLearnerAnswer(), item.correctAnswer(), item.correct(), item.evidenceReference(),
                item.explanation(), score.scoringPolicyVersion(), Instant.now())).toList();
        results.saveAll(questionResults);
        PracticeSubmission graded = submissions.markScored(submissionId, Instant.now());
        return new ObjectiveSubmissionResult(graded, score);
    }

    private ObjectiveScore summarize(List<QuestionResult> questionResults) {
        int correct = (int) questionResults.stream().filter(QuestionResult::correct).count();
        List<QuestionScore> scores = questionResults.stream().map(item -> new QuestionScore(item.questionId(), item.questionType(),
                item.learnerAnswer(), item.normalizedLearnerAnswer(), item.correctAnswer(), item.correct(),
                item.evidenceReference(), item.explanation())).toList();
        return new ObjectiveScore(correct, questionResults.size(), null,
                questionResults.getFirst().scoringPolicyVersion(), scores);
    }

    private boolean isObjective(String skill) { return "READING".equalsIgnoreCase(skill) || "LISTENING".equalsIgnoreCase(skill); }
}
