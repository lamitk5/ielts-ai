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
        @Size(max = 4000) String selectedText
) {
    public String normalizedSkill() {
        return skill == null || skill.isBlank() ? "GENERAL" : skill;
    }
}
