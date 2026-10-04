package com.ieltsaitutor.assessment.objective;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.SyntheticPracticeCatalog;
import com.ieltsaitutor.practice.PracticeQuestion;
import com.ieltsaitutor.practice.PracticeSet;
import com.ieltsaitutor.practice.catalog.ApprovedPracticeCatalogService;
import com.ieltsaitutor.practice.catalog.PracticePublication;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.PracticeSubmissionRepository;
import com.ieltsaitutor.submission.SubmissionAnswerRepository;
import com.ieltsaitutor.submission.SubmissionStatus;

class DeterministicPracticeWithoutAiE2ETest {
    @Test
    void unapprovedPracticeCannotReachObjectiveScoring() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(new PracticeSubmission(submissionId, owner,
                "READING", "unapproved", "version", "unapproved", 1, SubmissionStatus.SUBMITTED,
                Instant.now(), Instant.now(), Instant.now(), null, 0, "start", "submit", "hash", false, Instant.now(), Instant.now())));
        ApprovedPracticeCatalogService approved = mock(ApprovedPracticeCatalogService.class);
        when(approved.findActive("unapproved")).thenReturn(Optional.empty());
        QuestionResultRepository results = mock(QuestionResultRepository.class);
        ObjectiveSubmissionScoringService service = new ObjectiveSubmissionScoringService(submissions, mock(SubmissionAnswerRepository.class),
                results, mock(SyntheticPracticeCatalog.class), new DeterministicObjectiveScorer(), mock(ObjectiveResultPublisher.class), approved);

        assertThrows(Exception.class, () -> service.score(owner, submissionId));
        verify(results, never()).saveAll(any());
    }

    @Test
    void forgedClientScoreIsNotReadByObjectiveScoringBoundary() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeSubmission submitted = new PracticeSubmission(submissionId, owner, "READING", "approved", "v1", "approved", 1,
                SubmissionStatus.SUBMITTED, now, now, now, null, 0, "start", "submit", "hash", false, now, now);
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionAnswerRepository answers = mock(SubmissionAnswerRepository.class);
        QuestionResultRepository results = mock(QuestionResultRepository.class);
        SyntheticPracticeCatalog catalog = mock(SyntheticPracticeCatalog.class);
        ApprovedPracticeCatalogService approved = mock(ApprovedPracticeCatalogService.class);
        when(submissions.findByOwnerAndId(owner, submissionId)).thenReturn(Optional.of(submitted));
        when(answers.findBySubmissionId(submissionId)).thenReturn(Optional.of(new com.ieltsaitutor.submission.SubmissionAnswerSnapshot(
                submissionId, owner, Map.of("q1", "B", "score", "100"), "hash", now)));
        when(results.findByOwnedSubmission(owner, submissionId)).thenReturn(java.util.List.of());
        when(approved.findActive("approved")).thenReturn(Optional.of(new PracticePublication("approved", UUID.randomUUID(),
                UUID.randomUUID(), "reading", true, 1, "qa", now)));
        when(catalog.find("reading", "approved")).thenReturn(new PracticeSet("approved", "reading", "Reading", "",
                java.util.List.of(new PracticeQuestion("q1", "Which?", java.util.List.of("A", "B"), "A", ""))));
        when(submissions.markScored(any(), any())).thenReturn(submitted.withFinalized(now, "submit", "hash"));

        ObjectiveSubmissionScoringService service = new ObjectiveSubmissionScoringService(submissions, answers, results, catalog,
                new DeterministicObjectiveScorer(), mock(ObjectiveResultPublisher.class), approved);
        org.junit.jupiter.api.Assertions.assertEquals(0, service.score(owner, submissionId).score().correctCount());
        org.mockito.ArgumentCaptor<java.util.List<QuestionResult>> captured = org.mockito.ArgumentCaptor.forClass(java.util.List.class);
        verify(results).saveAll(captured.capture());
        org.junit.jupiter.api.Assertions.assertEquals(false, captured.getValue().getFirst().correct());
    }
}
