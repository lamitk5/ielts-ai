package com.ieltsaitutor.results;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.assessment.objective.QuestionResult;
import com.ieltsaitutor.submission.PracticeSubmission;
import com.ieltsaitutor.submission.SubmissionStatus;

class LearnerResultViewMapperTest {
    @Test
    void mapsTrustedObjectiveResultAndKeepsEstimateBoundary() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeSubmission submission = new PracticeSubmission(submissionId, owner, "READING", "set", "v1", "set", 1,
                SubmissionStatus.GRADED, now, now, now, now, 0, "start", "submit", "hash", false, now, now);
        LearnerResult result = ResultViewMapper.objective(submission, List.of(new QuestionResult(UUID.randomUUID(), submissionId,
                owner, "q1", "MULTIPLE_CHOICE", "A", "A", "A", true, "passage:p1", "", "objective-v1", now)));

        assertEquals(ResultStatus.READY, result.resultStatus());
        assertEquals(1, result.score());
        assertEquals(1, result.total());
        assertNull(result.estimatedBand());
        assertNull(result.estimatedBandLabel());
    }

    @Test
    void hidesAnswerKeyBeforeTrustedGrade() {
        UUID owner = UUID.randomUUID();
        UUID submissionId = UUID.randomUUID();
        Instant now = Instant.now();
        PracticeSubmission submission = new PracticeSubmission(submissionId, owner, "LISTENING", "set", "v1", "set", 1,
                SubmissionStatus.SUBMITTED, now, now, now, null, 0, "start", "submit", "hash", false, now, now);
        LearnerResult result = ResultViewMapper.objective(submission, List.of());

        assertEquals(ResultStatus.PROCESSING, result.resultStatus());
        assertNull(result.score());
        assertEquals(List.of(), result.questionResults());
    }
}
