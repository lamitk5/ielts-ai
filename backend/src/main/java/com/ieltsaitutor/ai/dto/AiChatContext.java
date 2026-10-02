package com.ieltsaitutor.ai.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiChatContext(
        @Pattern(regexp = "GENERAL|READING|LISTENING|WRITING|SPEAKING", message = "context.skill is not supported")
        String skill,
        @Size(max = 120) String lessonId,
        @Size(max = 120) String exerciseId,
        @Size(max = 120) String questionId,
        @Size(max = 40) String taskType,
        @Size(max = 120) String errorLocation,
        @Size(max = 4000) String selectedText,
        @Size(max = 80) String attemptId,
        @Size(max = 120) String promptId,
        @Size(max = 80) String resultId
) {
    public AiChatContext(String skill, String lessonId, String exerciseId, String questionId, String taskType,
            String errorLocation, String selectedText) {
        this(skill, lessonId, exerciseId, questionId, taskType, errorLocation, selectedText, null, null, null);
    }

    public AiChatContext(String skill, String lessonId, String exerciseId, String questionId, String taskType,
            String errorLocation, String selectedText, String attemptId, String promptId) {
        this(skill, lessonId, exerciseId, questionId, taskType, errorLocation, selectedText, attemptId, promptId, null);
    }

    public String normalizedSkill() {
        return skill == null || skill.isBlank() ? "GENERAL" : skill;
    }
}
