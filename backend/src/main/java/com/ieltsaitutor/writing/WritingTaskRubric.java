package com.ieltsaitutor.writing;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record WritingTaskRubric(
        String taskType,
        List<String> criterionKeys) {

    public WritingTaskRubric {
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(criterionKeys, "criterionKeys must not be null");
        criterionKeys = List.copyOf(criterionKeys);
    }

    public boolean hasCriterion(String criterionKey) {
        return criterionKeys.contains(criterionKey);
    }

    public boolean isValidCriteria(Map<String, ?> criteria) {
        if (criteria == null) {
            return false;
        }
        Set<String> keys = criteria.keySet();
        return keys.size() == criterionKeys.size() && keys.containsAll(criterionKeys);
    }
}
