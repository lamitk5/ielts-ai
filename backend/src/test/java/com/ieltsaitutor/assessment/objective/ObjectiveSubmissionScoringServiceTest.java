package com.ieltsaitutor.assessment.objective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.PracticeQuestion;
import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionAnswerRepository;
import com.ieltsaitutor.submission.SubmissionAnswerSnapshot;
import com.ieltsaitutor.submission.SubmissionStatus;

class ObjectiveSubmissionScoringServiceTest {
    @Test
    void scoresReadingFromApprovedServerQuestionSetAndNeverUsesClientScore() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeSubmission submitted = submission(owner, submissionId, SubmissionStatus.SUBMITTED);
        PracticeSubmission graded = submission(owner, submissionId, SubmissionStatus.GRADED);
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionAnswerRepository answers = mock(SubmissionAnswerRepository.class);
        QuestionResultRepository results = mock(QuestionResultRepository.class);
        SyntheticPracticeCatalog catalog = mock(SyntheticPracticeCatalog.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(submitted));
        when(answers.findBySubmissionId(submissionId)).thenReturn(Optional.of(new SubmissionAnswerSnapshot(submissionId, owner,
                Map.of("q1", "B"), "hash", now)));
        when(results.findByOwnedSubmission(owner, submissionId)).thenReturn(List.of());
        when(catalog.find("reading", "reading-approved")).thenReturn(new PracticeSet("reading-approved", "reading", "Reading", "",
                List.of(new PracticeQuestion("q1", "Which?", List.of("A", "B"), "A", "why"))));
        when(submissions.markScored(any(), any())).thenReturn(graded);
        ObjectiveSubmissionScoringService service = new ObjectiveSubmissionScoringService(submissions, answers, results, catalog,
                new DeterministicObjectiveScorer());

        ObjectiveSubmissionResult result = service.score(owner, submissionId);

        assertEquals(0, result.score().correctCount());
        assertEquals(SubmissionStatus.GRADED, result.submission().status());
        verify(results).saveAll(any());
    }

    @Test
    void rejectsNonObjectiveSubmissionWithoutScoring() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(submission(owner, submissionId, SubmissionStatus.SUBMITTED, "WRITING")));
        QuestionResultRepository results = mock(QuestionResultRepository.class);
        ObjectiveSubmissionScoringService service = new ObjectiveSubmissionScoringService(submissions, mock(SubmissionAnswerRepository.class),
                results, mock(SyntheticPracticeCatalog.class), new DeterministicObjectiveScorer());

        assertThrows(Exception.class, () -> service.score(owner, submissionId));
        verify(results, never()).saveAll(any());
    }

    private static PracticeSubmission submission(UUID owner, UUID id, SubmissionStatus status) {
        return submission(owner, id, status, "READING");
    }

    private static PracticeSubmission submission(UUID owner, UUID id, SubmissionStatus status, String skill) {
        Instant now = Instant.now();
        return new PracticeSubmission(id, owner, skill, "reading-approved", "v1", "reading-approved", 1, status,
                now, now, now, status == SubmissionStatus.GRADED ? now : null, 0, "start", "submit", "hash", false, now, now);
    }
}
