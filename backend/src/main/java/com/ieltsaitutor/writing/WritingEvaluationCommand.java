package com.ieltsaitutor.writing;

import java.util.Objects;
import java.util.UUID;

public record WritingEvaluationCommand(
        UUID submissionId,
        UUID versionId,
        String taskId,
        String responseText,
        WritingTaskRubric rubric) {

    public WritingEvaluationCommand {
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(versionId, "versionId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(responseText, "responseText must not be null");
        Objects.requireNonNull(rubric, "rubric must not be null");
    }
}
