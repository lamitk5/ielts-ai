package com.ieltsaitutor.submission;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class SubmissionStateMachine {
    private static final Map<SubmissionStatus, Set<SubmissionStatus>> RETRY_TARGETS = Map.of(
            SubmissionStatus.FAILED,
            EnumSet.of(SubmissionStatus.SCORING, SubmissionStatus.AI_EVALUATING, SubmissionStatus.PENDING_REVIEW));

    private final Map<SubmissionStatus, Map<SubmissionCommand, SubmissionStatus>> transitions = transitions();

    public SubmissionStatus next(SubmissionStatus current, SubmissionCommand command) {
        if (current == null || command == null) {
            throw new SubmissionConflictException("Submission state and command are required");
        }
        SubmissionStatus next = transitions.getOrDefault(current, Map.of()).get(command);
        if (next == null) {
            throw new SubmissionConflictException("Illegal submission transition: " + current + " + " + command);
        }
        return next;
    }

    public SubmissionStatus retry(SubmissionStatus current, SubmissionStatus target) {
        if (!RETRY_TARGETS.getOrDefault(current, Set.of()).contains(target)) {
            throw new SubmissionConflictException("Illegal submission retry target: " + current + " -> " + target);
        }
        return target;
    }

    private Map<SubmissionStatus, Map<SubmissionCommand, SubmissionStatus>> transitions() {
        Map<SubmissionStatus, Map<SubmissionCommand, SubmissionStatus>> result = new EnumMap<>(SubmissionStatus.class);
        result.put(SubmissionStatus.DRAFT, Map.of(
                SubmissionCommand.START, SubmissionStatus.IN_PROGRESS,
                SubmissionCommand.BEGIN, SubmissionStatus.IN_PROGRESS,
                SubmissionCommand.FAIL, SubmissionStatus.FAILED));
        result.put(SubmissionStatus.IN_PROGRESS, Map.of(
                SubmissionCommand.SUBMIT, SubmissionStatus.SUBMITTED,
                SubmissionCommand.FAIL, SubmissionStatus.FAILED));
        result.put(SubmissionStatus.SUBMITTED, Map.of(
                SubmissionCommand.BEGIN_SCORING, SubmissionStatus.SCORING,
                SubmissionCommand.BEGIN_AI_EVALUATION, SubmissionStatus.AI_EVALUATING,
                SubmissionCommand.BEGIN_REVIEW, SubmissionStatus.PENDING_REVIEW,
                SubmissionCommand.GRADE, SubmissionStatus.GRADED));
        result.put(SubmissionStatus.SCORING, Map.of(
                SubmissionCommand.GRADE, SubmissionStatus.GRADED,
                SubmissionCommand.FAIL, SubmissionStatus.FAILED));
        result.put(SubmissionStatus.AI_EVALUATING, Map.of(
                SubmissionCommand.GRADE, SubmissionStatus.GRADED,
                SubmissionCommand.FAIL, SubmissionStatus.FAILED));
        result.put(SubmissionStatus.PENDING_REVIEW, Map.of(
                SubmissionCommand.GRADE, SubmissionStatus.GRADED,
                SubmissionCommand.FAIL, SubmissionStatus.FAILED));
        return result;
    }
}
