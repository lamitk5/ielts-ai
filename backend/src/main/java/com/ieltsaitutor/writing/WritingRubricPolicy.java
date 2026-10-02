package com.ieltsaitutor.writing;

import java.util.List;
import java.util.Locale;

public final class WritingRubricPolicy {

    public static final List<String> TASK_1_CRITERIA = List.of(
            "taskAchievement",
            "coherenceCohesion",
            "lexicalResource",
            "grammaticalRangeAccuracy");

    public static final List<String> TASK_2_CRITERIA = List.of(
            "taskResponse",
            "coherenceCohesion",
            "lexicalResource",
            "grammaticalRangeAccuracy");

    private WritingRubricPolicy() {}

    public static WritingTaskRubric resolveRubric(String taskId) {
        String taskType = resolveTaskType(taskId);
        if ("TASK_1".equals(taskType)) {
            return new WritingTaskRubric("TASK_1", TASK_1_CRITERIA);
        } else if ("TASK_2".equals(taskType)) {
            return new WritingTaskRubric("TASK_2", TASK_2_CRITERIA);
        }
        throw new IllegalArgumentException("Unknown or unsupported writing task ID: " + taskId);
    }

    public static String resolveTaskType(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return null;
        }
        String normalized = taskId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("task-1") || normalized.startsWith("task1") || normalized.contains("task_1")) {
            return "TASK_1";
        }
        if (normalized.startsWith("task-2") || normalized.startsWith("task2") || normalized.contains("task_2")) {
            return "TASK_2";
        }
        return null;
    }
}
