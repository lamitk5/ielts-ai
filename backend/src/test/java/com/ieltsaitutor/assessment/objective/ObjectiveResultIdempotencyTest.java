package com.ieltsaitutor.assessment.objective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

class ObjectiveResultIdempotencyTest {
    @Test
    void repeatedScoringReadsImmutableResultsAndPublishesTrustedEventOnce() {
        UUID owner = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeSubmission submitted = submission(owner, id, SubmissionStatus.SUBMITTED);
        PracticeSubmission graded = submission(owner, id, SubmissionStatus.GRADED);
        PracticeSubmissionRepository submissions = mock(PracticeSubmissionRepository.class);
        SubmissionAnswerRepository answers = mock(SubmissionAnswerRepository.class);
        QuestionResultRepository results = mock(QuestionResultRepository.class);
        SyntheticPracticeCatalog catalog = mock(SyntheticPracticeCatalog.class);
        ObjectiveResultPublisher publisher = mock(ObjectiveResultPublisher.class);
        when(submissions.findByOwnerAndId(owner, id)).thenReturn(Optional.of(submitted), Optional.of(graded));
        when(answers.findBySubmissionId(id)).thenReturn(Optional.of(new SubmissionAnswerSnapshot(id, owner, Map.of("q1", "A"), "hash", now)));
        when(results.findByOwnedSubmission(owner, id)).thenReturn(List.of(), List.of(new QuestionResult(UUID.randomUUID(), id, owner,
                "q1", "MULTIPLE_CHOICE", "A", "A", "A", true, "", "", "objective-v1", now)));
        when(catalog.find("reading", "reading-approved")).thenReturn(new PracticeSet("reading-approved", "reading", "Reading", "",
                List.of(new PracticeQuestion("q1", "Which?", List.of("A"), "A", ""))));
        when(submissions.markScored(any(), any())).thenReturn(graded);
        ObjectiveSubmissionScoringService service = new ObjectiveSubmissionScoringService(submissions, answers, results, catalog,
                new DeterministicObjectiveScorer(), publisher);

        assertEquals(1, service.score(owner, id).score().correctCount());
        assertEquals(1, service.score(owner, id).score().correctCount());
        verify(publisher, times(1)).publish(any(), any(), any());
        verify(results, times(1)).saveAll(any());
    }

    private static PracticeSubmission submission(UUID owner, UUID id, SubmissionStatus status) {
        Instant now = Instant.now();
        return new PracticeSubmission(id, owner, "READING", "reading-approved", "v1", "reading-approved", 1, status,
                now, now, now, status == SubmissionStatus.GRADED ? now : null, 0, "start", "submit", "hash", false, now, now);
    }
}
