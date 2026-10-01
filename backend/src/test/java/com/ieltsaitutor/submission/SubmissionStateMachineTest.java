package com.ieltsaitutor.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SubmissionStateMachineTest {
    private final SubmissionStateMachine machine = new SubmissionStateMachine();

    @Test
    void allowsOnlyTheApprovedCommonLifecycle() {
        assertEquals(SubmissionStatus.IN_PROGRESS,
                machine.next(SubmissionStatus.DRAFT, SubmissionCommand.BEGIN));
        assertEquals(SubmissionStatus.SUBMITTED,
                machine.next(SubmissionStatus.IN_PROGRESS, SubmissionCommand.SUBMIT));
        assertEquals(SubmissionStatus.SCORING,
                machine.next(SubmissionStatus.SUBMITTED, SubmissionCommand.BEGIN_SCORING));
        assertEquals(SubmissionStatus.GRADED,
                machine.next(SubmissionStatus.SCORING, SubmissionCommand.GRADE));
        assertEquals(SubmissionStatus.FAILED,
                machine.next(SubmissionStatus.SCORING, SubmissionCommand.FAIL));
    }

    @Test
    void supportsSkillSpecificProcessingAndRetryTargets() {
        assertEquals(SubmissionStatus.AI_EVALUATING,
                machine.next(SubmissionStatus.SUBMITTED, SubmissionCommand.BEGIN_AI_EVALUATION));
        assertEquals(SubmissionStatus.PENDING_REVIEW,
                machine.next(SubmissionStatus.SUBMITTED, SubmissionCommand.BEGIN_REVIEW));
        assertEquals(SubmissionStatus.SCORING,
                machine.retry(SubmissionStatus.FAILED, SubmissionStatus.SCORING));
        assertEquals(SubmissionStatus.AI_EVALUATING,
                machine.retry(SubmissionStatus.FAILED, SubmissionStatus.AI_EVALUATING));
    }

    @Test
    void rejectsIllegalTransitionsAndRetryFromNonFailedState() {
        assertThrows(SubmissionConflictException.class,
                () -> machine.next(SubmissionStatus.GRADED, SubmissionCommand.SUBMIT));
        assertThrows(SubmissionConflictException.class,
                () -> machine.next(SubmissionStatus.DRAFT, SubmissionCommand.GRADE));
        assertThrows(SubmissionConflictException.class,
                () -> machine.retry(SubmissionStatus.IN_PROGRESS, SubmissionStatus.SCORING));
        assertThrows(SubmissionConflictException.class,
                () -> machine.retry(SubmissionStatus.FAILED, SubmissionStatus.GRADED));
    }
}
